package fun.democraft.smart.api;

import java.time.Instant;

/**
 * A row of the Smart core's server registry (Haze table {@code smart_servers}).
 * SmartServerRegistry maps the database columns to these fields.
 */
public record SmartServer(
        String id,
        String providerId,
        String familyId,
        String rcServerId,
        String status,
        long ramAmount,
        Instant createdAt
) {
    public enum Status {
        PROVISIONING,
        ONLINE,
        OFFLINE,
        DEPROVISIONING
    }
}
