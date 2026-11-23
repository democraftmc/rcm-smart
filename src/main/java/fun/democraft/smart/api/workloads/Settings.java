package fun.democraft.smart.api.workloads;

import fun.democraft.smart.api.SmartMetadata;
import group.aelysium.rustyconnector.proxy.family.Family;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public record Settings(Family family, String root, long sharedMemoryMB) {
    private static final Set<String> COMMON = Set.of("memory", "cpu", "env");
    public static final long BYTES_PER_MB = 1_000_000;
    public static final long BYTES_PER_MIB = 1_048_576;

    public static Settings read(Family family, String id, Set<String> fields) {
        String root = "smart." + id;
        if (!(SmartMetadata.fetch(family, root).orElse(null) instanceof Map<?, ?> block)) {
            throw new IllegalArgumentException(root + " must be a YAML object");
        }
        for (Object key : block.keySet()) {
            if (!(key instanceof String name) || (!COMMON.contains(name) && !fields.contains(name))) {
                throw new IllegalArgumentException(root + "." + key + " is unsupported");
            }
        }
        // Smart reads this again to record the allocation, even if the provider overrides RAM.
        long memory = SmartMetadata.memory(family, 1024);
        if (memory <= 0) throw new IllegalArgumentException("smart.memory must be positive (MB)");
        return new Settings(family, root, memory);
    }

    public Optional<Object> value(String key) {
        return SmartMetadata.fetch(family, root + "." + key);
    }

    public String string(String key) {
        if (value(key).orElse(null) instanceof String text && !text.isBlank() && text.indexOf(0) < 0) {
            return text;
        }
        throw new IllegalArgumentException(root + "." + key + " must be a non-empty string");
    }

    public long integer(String key, Number fallback, long min, long max) {
        long result = scaled(root + "." + key, value(key).orElse(fallback), 1);
        if (result < min || result > max) {
            throw new IllegalArgumentException(root + "." + key + " must be between " + min + " and " + max);
        }
        return result;
    }

    public long memoryBytes() {
        if (value("memory").isPresent()) {
            return integer("memory", null, 1, Long.MAX_VALUE / BYTES_PER_MB) * BYTES_PER_MB;
        }
        return scaled("smart.memory", sharedMemoryMB, BYTES_PER_MB);
    }

    public String resourcePath(String key) {
        return value(key).isPresent() ? root + "." + key : "smart." + key;
    }

    public long cpu(long unitsPerCore, long unitsPerOverride) {
        return cpu(unitsPerCore, unitsPerOverride, 1, Long.MAX_VALUE);
    }

    public long cpu(long unitsPerCore, long unitsPerOverride, long min, long max) {
        var override = value("cpu");
        String path = override.isPresent() ? root + ".cpu" : "smart.cpu";
        Object input = override.orElseGet(() -> SmartMetadata.fetch(family, path).orElse(1));
        long units = scaled(path, input, override.isPresent() ? unitsPerOverride : unitsPerCore);
        if (units < min || units > max) {
            throw new IllegalArgumentException(path + " is outside the backend's CPU range");
        }
        return units;
    }

    private static long scaled(String path, Object input, long scale) {
        try {
            return new BigDecimal(String.valueOf(input)).multiply(BigDecimal.valueOf(scale)).longValueExact();
        } catch (ArithmeticException | NumberFormatException ignored) {
            throw new IllegalArgumentException(path + " is missing or cannot be expressed in the backend's units");
        }
    }

    public Map<String, String> env() {
        if (!(value("env").orElse(Map.of()) instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(root + ".env must be a YAML object");
        }
        Map<String, String> result = new TreeMap<>();
        for (Object key : map.keySet()) {
            if (!(key instanceof String name) || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException(root + ".env contains an invalid variable name");
            }
            Object value = value("env." + name).orElse(null);
            if (!(value instanceof String || value instanceof Boolean || value instanceof Number)
                    || value.toString().indexOf(0) >= 0) {
                throw new IllegalArgumentException(root + ".env." + name + " requires a scalar value without NUL");
            }
            result.put(name, value.toString());
        }
        return result;
    }
}
