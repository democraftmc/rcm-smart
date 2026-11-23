package fun.democraft.smart.api;

import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A Smart provider knows how to create and destroy real server processes
 * (e.g. through the Pterodactyl API) on behalf of the Smart core.
 * <p>
 * Per-family resources (RAM, provider-specific settings) are read from the
 * family's metadata via {@link SmartMetadata} — providers should use
 * {@link SmartMetadata#providerData(Family, String)} with their own id.
 * <p>
 * Implementations live in their own native modules (e.g. rcs-pterodactyl)
 * and are registered into the Smart core's provider registry at startup.
 */
public interface SmartProvider {
    /**
     * @return The unique id of this provider instance, matching its config file name.
     */
    String id();

    /**
     * @return Cosmetic name of this instance (e.g. "node-1-west"), for logs and status.
     * Defaults to {@link #id()}.
     */
    default String name() {
        return id();
    }

    /**
     * @return Families this provider is allowed to serve. Empty means all families.
     */
    Set<String> includedFamilies();

    /**
     * @return Families this provider must never serve. Wins over included families.
     */
    Set<String> excludedFamilies();

    /**
     * @return Maximum amount of server processes this provider may run at once.
     */
    int maxProcesses();

    /**
     * Providers can be registered multiple times under different ids (e.g. two
     * Pterodactyl instances for different regions). When several providers can serve
     * the same family, the one with the highest priority is tested first.
     *
     * @return Priority of this provider. Higher means tested first. Defaults to 0.
     */
    default int priority() {
        return 0;
    }

    /**
     * @return How many servers this provider is currently running.
     */
    int runningProcesses();

    /**
     * @return Whether this provider has capacity left for another server.
     */
    default boolean hasCapacity() {
        return runningProcesses() < maxProcesses();
    }

    /**
     * @param familyId The family a player wants to join.
     * @return Whether this provider is allowed to serve the family.
     */
    default boolean handlesFamily(String familyId) {
        if (excludedFamilies().contains(familyId)) return false;
        return includedFamilies().isEmpty() || includedFamilies().contains(familyId);
    }

    /**
     * Provision a new server for the given family.
     * The Smart core persists the result in its server registry.
     *
     * @param family The family the server is provisioned for. Read provider-specific
     *               settings from it via {@link SmartMetadata#providerData(Family, String)}.
     * @return The id of the newly created server (provider-side id).
     */
    String provision(@NotNull Family family) throws Exception;

    /**
     * Deprovision (delete) a previously provisioned server.
     *
     * @param serverId The provider-side id returned by {@link #provision(Family)}.
     */
    void deprovision(String serverId) throws Exception;
}
