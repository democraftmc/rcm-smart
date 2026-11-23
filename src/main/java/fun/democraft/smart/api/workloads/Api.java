package fun.democraft.smart.api.workloads;

import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonElement;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonNull;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class Api implements AutoCloseable {
    private static final Gson JSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private final String baseUrl;
    private final Map<String, String> headers;
    private final List<String> curl;
    private final HttpClient client;

    private Api(String baseUrl, Map<String, String> headers, List<String> curl, SSLContext tls) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.headers = headers;
        this.curl = List.copyOf(curl);
        var builder = HttpClient.newBuilder().connectTimeout(TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1);
        if (tls != null) builder.sslContext(tls);
        this.client = curl.isEmpty() ? builder.build() : null;
    }

    public static Api configured(String id, boolean docker, String authHeader, Map<String, String> env) {
        String prefix = "RC_" + id.toUpperCase(Locale.ROOT).replace('-', '_') + "_";
        String url = env.getOrDefault(prefix + "ENDPOINT", docker ? "unix:///var/run/docker.sock" : "");
        URI uri;
        try {
            uri = URI.create(url);
            boolean unix = docker && "unix".equals(uri.getScheme()) && uri.getAuthority() == null
                    && uri.getPath() != null && uri.getPath().startsWith("/");
            boolean http = ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null;
            if ((!unix && !http) || uri.getPort() > 65535 || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException(prefix + "ENDPOINT must be a supported endpoint URI");
        }

        if (docker) {
            // curl handles HTTP framing on Unix sockets and PEM client certificates on TCP.
            var args = new ArrayList<>(List.of("curl", "--disable", "--silent", "--globoff",
                    "--max-time", Long.toString(TIMEOUT.toSeconds())));
            if ("unix".equals(uri.getScheme())) {
                args.addAll(List.of("--unix-socket", uri.getPath(), "--noproxy", "*"));
                url = "http://localhost";
            }
            if (env.containsKey(prefix + "CERT") != env.containsKey(prefix + "KEY")) {
                throw new IllegalArgumentException(prefix + "CERT and " + prefix + "KEY must be set together");
            }
            for (var option : Map.of("CA", "--cacert", "CERT", "--cert", "KEY", "--key").entrySet()) {
                String file = env.get(prefix + option.getKey());
                if (file == null) continue;
                args.addAll(List.of(option.getValue(), pemPath(uri, prefix + option.getKey(), file).toString()));
            }
            boolean socketOrPem = "unix".equals(uri.getScheme()) || env.containsKey(prefix + "CA") || env.containsKey(prefix + "CERT");
            return new Api(url, Map.of(), socketOrPem ? args : List.of(), null);
        }

        String token = env.get(prefix + "TOKEN");
        if (token == null || !token.matches("[!-~]+")) {
            throw new IllegalArgumentException(prefix + "TOKEN must contain a printable API credential");
        }
        SSLContext tls = null;
        if (env.containsKey(prefix + "CA")) {
            String key = prefix + "CA";
            Path ca = pemPath(uri, key, env.get(key));
            try (var input = Files.newInputStream(ca)) {
                var trust = KeyStore.getInstance(KeyStore.getDefaultType());
                trust.load(null, null);
                var certificates = CertificateFactory.getInstance("X.509").generateCertificates(input);
                if (certificates.isEmpty()) throw new IOException();
                int index = 0;
                for (var certificate : certificates) trust.setCertificateEntry("ca-" + index++, certificate);
                var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                managers.init(trust);
                tls = SSLContext.getInstance("TLS");
                tls.init(null, managers.getTrustManagers(), null);
            } catch (Exception ignored) {
                throw new IllegalArgumentException(key + " must contain PEM-encoded CA certificates");
            }
        }
        String credential = authHeader.equals("Authorization") ? "Bearer " + token : token;
        return new Api(url, Map.of(authHeader, credential), List.of(), tls);
    }

    private static Path pemPath(URI uri, String key, String file) {
        try {
            Path path = Path.of(file).toAbsolutePath();
            if ("https".equals(uri.getScheme()) && Files.isRegularFile(path) && Files.isReadable(path)) return path;
        } catch (IllegalArgumentException ignored) {
            // Report the setting name without including its value.
        }
        throw new IllegalArgumentException(key + " requires HTTPS and a readable PEM file");
    }

    public JsonElement call(String method, String path, Object payload, boolean allowMissing) throws Exception {
        String body = payload == null ? null : JSON.toJson(payload);
        Response result;
        try {
            result = client == null ? curl(method, path, body) : http(method, path, body);
        } catch (TimeoutException ignored) {
            throw new IOException(method + " " + path + " timed out");
        } catch (IOException | ExecutionException ignored) {
            throw new IOException(method + " " + path + " failed at the HTTP transport");
        }
        if (allowMissing && result.status == 404) return JsonNull.INSTANCE;
        if (result.status < 200 || result.status >= 300) {
            throw new IOException(method + " " + path + " returned HTTP " + result.status);
        }
        try {
            return result.body.isBlank() ? JsonNull.INSTANCE : JsonParser.parseString(result.body);
        } catch (RuntimeException ignored) {
            throw new IOException("Malformed API response for " + path);
        }
    }

    private record Response(int status, String body) {}

    private Response http(String method, String path, String body) throws InterruptedException, ExecutionException, TimeoutException {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(TIMEOUT)
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(request::header);
        var pending = client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
        try {
            // Bound reading the complete response, including servers that stall after sending headers.
            var response = pending.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return new Response(response.statusCode(), response.body());
        } catch (InterruptedException | TimeoutException failure) {
            pending.cancel(true);
            throw failure;
        }
    }

    private Response curl(String method, String path, String body) throws Exception {
        var args = new ArrayList<>(curl);
        args.addAll(List.of("--request", method, "--write-out", "\n%{http_code}", "--header", "Content-Type: application/json"));
        if (body != null) args.addAll(List.of("--data-binary", "@-"));
        args.add(baseUrl + path);
        var process = new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            try (var stdin = process.getOutputStream()) {
                if (body != null) stdin.write(body.getBytes(StandardCharsets.UTF_8));
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit == 28) throw new TimeoutException();
            if (exit != 0) throw new IOException();
            int separator = output.lastIndexOf('\n');
            return new Response(Integer.parseInt(output.substring(separator + 1)), output.substring(0, separator));
        } finally {
            process.destroyForcibly();
        }
    }

    public static String responseId(JsonElement response, String... fields) throws IOException {
        for (String field : fields) {
            if (response == null || !response.isJsonObject()) throw new IOException("Missing API resource identifier");
            response = response.getAsJsonObject().get(field);
        }
        if (response == null || !response.isJsonPrimitive() || response.getAsJsonPrimitive().isBoolean()) {
            throw new IOException("Missing API resource identifier");
        }
        try {
            return identifier(response.getAsString());
        } catch (IllegalArgumentException ignored) {
            throw new IOException("Invalid API resource identifier");
        }
    }

    public static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid provider resource identifier");
        }
        return value;
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }
}
