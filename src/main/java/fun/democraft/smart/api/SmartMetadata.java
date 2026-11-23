package fun.democraft.smart.api;

import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the {@code "smart"} section of a family's metadata.
 * <p>
 * Family metadata is a JSON object defined on the family, e.g.:
 * <pre>{ "smart": { "memory": 2048, "pterodactyl": { "egg": 123, "apiKey": "..." } } }</pre>
 * Any path can be overridden by an environment variable; see {@link #envKeyFor(String)}.
 */
public final class SmartMetadata {
    public static final String ROOT_KEY = "smart";

    private SmartMetadata() {}

    /**
     * Fetches a value from the family's metadata by dotted path (e.g. {@code "smart.pterodactyl.egg"}).
     * <p>
     * An environment variable always wins over the config value: for a path {@code smart.pterodactyl.egg}
     * the env var {@code SMART_PTERODACTYL_EGG} is checked first (uppercase, dots become underscores,
     * leading {@code smart.} root stripped). If set, its value is returned — coerced to the type of the
     * config default when possible — otherwise the config value is used.
     *
     * @param family The family whose metadata should be read.
     * @param path   Dotted path into the metadata object.
     * @return The resolved value, or empty if neither env var nor config path exists.
     */
    public static @NotNull Optional<Object> fetch(@NotNull Family family, @NotNull String path) {
        String envKey = envKeyFor(path);
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            Object fallback = walk(family.metadata(), path);
            return Optional.of(coerce(envValue, fallback));
        }
        Object current = walk(family.metadata(), path);
        if (current == null) return Optional.empty();
        return Optional.of(current);
    }

    /**
     * Maps a dotted metadata path to its env override name:
     * {@code smart.pterodactyl.egg} → {@code SMART_PTERODACTYL_EGG}.
     */
    public static @NotNull String envKeyFor(@NotNull String path) {
        String p = path.startsWith(ROOT_KEY + ".") ? path.substring(ROOT_KEY.length() + 1) : path;
        return "SMART_" + p.toUpperCase().replace('.', '_');
    }

    private static Object walk(Map<String, Object> root, String path) {
        Object current = root;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(key);
            if (current == null) return null;
        }
        return current;
    }

    private static Object coerce(String envValue, Object fallback) {
        if (fallback instanceof Boolean) return Boolean.parseBoolean(envValue);
        if (fallback instanceof Number) {
            try {
                var number = new BigDecimal(envValue);
                if (fallback instanceof Long || fallback instanceof Integer) {
                    try {
                        return number.longValueExact();
                    } catch (ArithmeticException ignored) {
                        // Fractional CPU is valid even when the YAML default was an integer.
                        return number;
                    }
                }
                return number.doubleValue();
            } catch (NumberFormatException e) {
                return envValue;
            }
        }
        return envValue;
    }

    /**
     * Fetches a value and casts it, coercing numbers where sensible.
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull Optional<T> fetch(@NotNull Family family, @NotNull String path, @NotNull Class<T> type) {
        return fetch(family, path).map(v -> {
            if (type.isInstance(v)) return type.cast(v);
            if (v instanceof Number n && type == Long.class) return (T) Long.valueOf(n.longValue());
            if (v instanceof Number n && type == Integer.class) return (T) Integer.valueOf(n.intValue());
            if (v instanceof Number n && type == Double.class) return (T) Double.valueOf(n.doubleValue());
            return (T) String.valueOf(v);
        });
    }

    /**
     * @return The RAM (in MB) the family wants for its smart servers: {@code smart.memory}.
     */
    public static long memory(@NotNull Family family, long fallback) {
        Object value = fetch(family, ROOT_KEY + ".memory").orElse(fallback);
        try {
            return new BigDecimal(value.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException ignored) {
            throw new IllegalArgumentException("smart.memory must be a whole number of MB");
        }
    }

    /**
     * @return The provider-specific settings block: {@code smart.<providerId>} as a map.
     */
    public static @NotNull Optional<Map<String, Object>> providerData(@NotNull Family family, @NotNull String providerId) {
        return fetch(family, ROOT_KEY + "." + providerId).map(SmartMetadata::asMap);
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Map.of();
    }
}
