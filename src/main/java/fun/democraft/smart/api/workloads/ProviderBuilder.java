package fun.democraft.smart.api.workloads;

import group.aelysium.rustyconnector.common.modules.Module;
import fun.democraft.smart.ProviderConfig;
import fun.democraft.smart.SmartModule;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/** Builds and binds provider instances from a native module entry point. */
public final class ProviderBuilder {
    private ProviderBuilder() {}

    private record Instances(List<Provider> providers) implements Module {
        @Override public Component details() {
            return Component.text("Smart providers: " + providers.stream().map(Provider::id).toList());
        }
        @Override public void close() { providers.forEach(Provider::close); }
    }

    public static Module start(String type, Function<ProviderConfig, ? extends Provider> factory) {
        String prefix = "RC_" + type.toUpperCase(Locale.ROOT);
        String ids = System.getenv().getOrDefault(prefix + "_IDS", System.getenv().getOrDefault(prefix + "_ID", type));
        List<Provider> providers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            for (String item : ids.split(",", -1)) {
                String id = item.trim();
                if (!id.matches("[a-z0-9][a-z0-9-]{0,63}") || !seen.add(id))
                    throw new IllegalArgumentException(prefix + "_IDS requires distinct provider IDs");
                ProviderConfig config = ProviderConfig.New(id);
                if (config.providerId.isBlank()) config.providerId = id;
                if (!id.equals(config.providerId)) throw new IllegalArgumentException(id + ".yml providerId mismatch");
                providers.add(factory.apply(config));
            }
        } catch (RuntimeException failure) {
            providers.forEach(Provider::close);
            throw failure;
        }
        return new Instances(List.copyOf(providers));
    }

    public static void bind(ProxyKernel kernel, Module module) {
        SmartModule smart = (SmartModule) kernel.fetchModule("Smart").orElseThrow();
        ((Instances) module).providers.forEach(smart::registerProvider);
    }
}
