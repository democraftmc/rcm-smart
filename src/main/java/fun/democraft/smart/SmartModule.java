package fun.democraft.smart;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.modules.stats.RustyContext;
import group.aelysium.rustyconnector.shaded.dev.faststats.Metrics;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.modules.Module;
import fun.democraft.smart.api.SmartMetadata;
import fun.democraft.smart.api.SmartProvider;
import fun.democraft.smart.api.SmartServer;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Smart core module. Owns the provider registry and the Haze-backed
 * server registry, and exposes the high-level provision/deprovision API
 * that provider modules and event listeners use.
 */
public class SmartModule implements Module {
    private final SmartConfig config;
    private final ProviderRegistry providerRegistry = new ProviderRegistry();
    private final SmartServerRegistry serverRegistry;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Object provisioningLock = new Object();
    private StartupProvisioning startupProvisioning;
    private final RustyContext context = new RustyContext.Factory(this, "8bced7c4b0b146cfaed68d573053af1c")
            .metrics(Metrics.Factory::create)
            .create();

    public SmartModule(@NotNull SmartConfig config, @NotNull Flux<HazeDatabase> database) {
        this.config = config;
        this.context.ready();
        this.serverRegistry = new SmartServerRegistry(database);
    }

    public boolean isActive() {
        return this.active.get();
    }

    public @NotNull SmartConfig config() {
        return this.config;
    }

    public @NotNull ProviderRegistry providers() {
        return this.providerRegistry;
    }

    public @NotNull SmartServerRegistry servers() {
        return this.serverRegistry;
    }

    /**
     * Register a provider implementation. Called by provider modules during their bind phase.
     */
    public synchronized void registerProvider(@NotNull SmartProvider provider) {
        if (!this.isActive()) return;
        this.providerRegistry.register(provider);
        if (this.startupProvisioning != null) this.startupProvisioning.schedule();
    }

    synchronized void startStartupProvisioning(EventManager events) {
        if (!this.isActive() || this.startupProvisioning != null || this.config.startupServers.isEmpty()) return;
        this.startupProvisioning = new StartupProvisioning(this);
        events.listen(this.startupProvisioning);
        this.startupProvisioning.schedule();
    }

    /**
     * Provision a new server for the given family through the best matching provider.
     * RAM is read from the family's metadata ({@code smart.memory}).
     *
     * @return The registered {@link SmartServer}, or empty if no provider could serve the family.
     */
    public @NotNull Optional<SmartServer> provision(@NotNull Family family) {
        synchronized (this.provisioningLock) {
            if (!this.isActive()) return Optional.empty();
            try {
                Optional<SmartProvider> provider = this.providerRegistry.findForFamily(family.id());
                if (provider.isPresent()) return Optional.of(this.provision(family, provider.get()));
            } catch (Exception e) {
                RC.Error(Error.from(e).whileAttempting("To provision a server for family " + family.id()));
            }
            return Optional.empty();
        }
    }

    /**
     * Provision on demand when the family has no entries in the Smart registry.
     */
    public void provisionIfEmpty(@NotNull Family family) throws Exception {
        synchronized (this.provisioningLock) {
            if (this.isActive() && this.serverRegistry.serversByFamily(family.id()).isEmpty()) {
                this.provision(family);
            }
        }
    }

    boolean fillStartupTarget(Family family, int target) throws Exception {
        while (this.isActive()) {
            synchronized (this.provisioningLock) {
                if (!this.isActive()) return false;
                long tracked = this.serverRegistry.serversByFamily(family.id()).stream()
                        .filter(server -> SmartServer.Status.ONLINE.name().equals(server.status())
                                || SmartServer.Status.PROVISIONING.name().equals(server.status()))
                        .count();
                if (tracked >= target) return true;

                Optional<SmartProvider> provider = this.providerRegistry.findForFamily(family.id());
                if (provider.isEmpty()) {
                    RC.Error(Error.from("Smart startup target for " + family.id() + " is at " + tracked + "/" + target
                            + ". No matching provider has capacity.").urgent(true));
                    return false;
                }
                if (!this.isActive()) return false;
                this.provision(family, provider.get());
                if (tracked + 1 == target) return true;
            }
        }
        return false;
    }

    private SmartServer provision(Family family, SmartProvider provider) throws Exception {
        long memory = SmartMetadata.memory(family, 1024);
        String providerServerId = provider.provision(family);
        SmartServer server = new SmartServer(
                UUID.randomUUID().toString(),
                provider.id(),
                family.id(),
                providerServerId,
                SmartServer.Status.PROVISIONING.name(),
                memory,
                Instant.now()
        );
        try {
            this.serverRegistry.register(server);
        } catch (Exception e) {
            throw new IllegalStateException("Could not record server " + providerServerId + " from provider "
                    + provider.id() + " in the Smart registry: " + e.getMessage(), e);
        }
        return server;
    }

    /**
     * Deprovision a server: tell the provider to destroy it, then remove it from the registry.
     */
    public void deprovision(@NotNull String serverId) {
        try {
            Optional<SmartServer> server = this.serverRegistry.fetch(serverId);
            if (server.isEmpty()) return;

            SmartServer s = server.get();
            this.serverRegistry.updateStatus(s.id(), SmartServer.Status.DEPROVISIONING);

            this.providerRegistry.fetch(s.providerId()).ifPresent(p -> {
                try {
                    p.deprovision(s.rcServerId());
                } catch (Exception e) {
                    RC.Error(group.aelysium.rustyconnector.common.errors.Error.from(e)
                            .whileAttempting("To deprovision server " + s.rcServerId()));
                }
            });

            this.serverRegistry.unregister(s.id());
        } catch (Exception e) {
            RC.Error(group.aelysium.rustyconnector.common.errors.Error.from(e)
                    .whileAttempting("To deprovision smart server " + serverId));
        }
    }

    @Override
    public @Nullable Component details() {
        return Component.text("Smart core active with " + this.providerRegistry.providers().size() + " provider(s).");
    }

    @Override
    public synchronized void close() throws Exception {
        this.active.set(false);
        this.context.shutdown();
        if (this.startupProvisioning != null) this.startupProvisioning.close();
        this.providerRegistry.providers().forEach(p -> this.providerRegistry.unregister(p.id()));
    }
}
