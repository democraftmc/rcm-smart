package fun.democraft.smart;

import fun.democraft.smart.api.SmartProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds every {@link SmartProvider} registered by provider modules.
 */
public class ProviderRegistry {
    private final ConcurrentHashMap<String, SmartProvider> providers = new ConcurrentHashMap<>();

    public void register(@NotNull SmartProvider provider) {
        this.providers.put(provider.id(), provider);
    }

    public void unregister(@NotNull String providerId) {
        this.providers.remove(providerId);
    }

    public @NotNull Set<SmartProvider> providers() {
        return Set.copyOf(this.providers.values());
    }

    public @NotNull Optional<SmartProvider> fetch(@NotNull String providerId) {
        return Optional.ofNullable(this.providers.get(providerId));
    }

    /**
     * Picks the provider that is allowed to serve the family, still has capacity,
     * and has the highest priority (highest priority is tested first).
     */
    public @NotNull Optional<SmartProvider> findForFamily(@NotNull String familyId) {
        return this.providers.values().stream()
                .filter(p -> p.handlesFamily(familyId) && p.hasCapacity())
                .max(java.util.Comparator.comparingInt(SmartProvider::priority));
    }
}
