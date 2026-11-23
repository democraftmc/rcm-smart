package fun.democraft.smart;

import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import fun.democraft.smart.api.SmartServer;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataEntry;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataHolder;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Type;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.CreateRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.DeleteRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.ReadRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.UpdateRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Haze-backed registry of every server provisioned through Smart.
 * This is the single source of truth so provider modules never need their own cache.
 */
public class SmartServerRegistry {
    public static final String TABLE = "smart_servers";

    private final Flux<HazeDatabase> database;

    public SmartServerRegistry(@NotNull Flux<HazeDatabase> database) {
        this.database = database;
    }

    public void init() throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        if (db.doesDataHolderExist(TABLE)) return;

        DataHolder table = new DataHolder(TABLE);
        table.addKey("id", Type.STRING(64).primaryKey(true).nullable(false));
        table.addKey("provider_id", Type.STRING(64).nullable(false));
        table.addKey("family_id", Type.STRING(64).nullable(false));
        table.addKey("rc_server_id", Type.STRING(64).nullable(true));
        table.addKey("status", Type.STRING(32).nullable(false));
        table.addKey("ram_amount", Type.UNSIGNED_INTEGER().nullable(false));
        table.addKey("created_at", Type.DATETIME().nullable(false));
        db.createDataHolder(table);
    }

    public void register(@NotNull SmartServer server) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        CreateRequest query = db.newCreateRequest(TABLE);
        query.parameter("id", server.id());
        query.parameter("provider_id", server.providerId());
        query.parameter("family_id", server.familyId());
        query.parameter("rc_server_id", server.rcServerId());
        query.parameter("status", server.status());
        query.parameter("ram_amount", server.ramAmount());
        query.parameter("created_at", LocalDateTime.ofInstant(server.createdAt(), ZoneOffset.UTC));
        try {
            query.execute();
        } catch (Exception insertError) {
            // SQL drivers can fail while reading a numeric generated key after committing our string ID.
            if (insertError instanceof InterruptedException) throw insertError;
            try {
                Optional<SmartServer> persisted = this.fetch(server.id());
                if (persisted.filter(row -> Objects.equals(row.providerId(), server.providerId())
                        && Objects.equals(row.familyId(), server.familyId())
                        && Objects.equals(row.rcServerId(), server.rcServerId())).isPresent()) return;
            } catch (Exception readError) {
                if (readError instanceof InterruptedException) Thread.currentThread().interrupt();
                insertError.addSuppressed(readError);
            }
            throw insertError;
        }
    }

    public void updateStatus(@NotNull String id, @NotNull SmartServer.Status status) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        UpdateRequest query = db.newUpdateRequest(TABLE);
        query.parameter("status", status.name());
        query.withFilter(Filter.by("id", id, Filter.EQUALS));
        query.execute();
    }

    public void updateRcServerId(@NotNull String id, @NotNull String rcServerId) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        UpdateRequest query = db.newUpdateRequest(TABLE);
        query.parameter("rc_server_id", rcServerId);
        query.withFilter(Filter.by("id", id, Filter.EQUALS));
        query.execute();
    }

    public void unregister(@NotNull String id) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        DeleteRequest query = db.newDeleteRequest(TABLE);
        query.withFilter(Filter.by("id", id, Filter.EQUALS));
        query.execute();
    }

    public @NotNull Set<SmartServer> servers() throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        ReadRequest query = db.newReadRequest(TABLE);
        // The SQL drivers require a filter. Every row has a non-null string ID.
        query.withFilter(Filter.by("id", "", Filter.GREATER_THAN_OR_EQUAL));
        return this.readServers(query);
    }

    public @NotNull Set<SmartServer> serversByFamily(@NotNull String familyId) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        ReadRequest query = db.newReadRequest(TABLE);
        query.withFilter(Filter.by("family_id", familyId, Filter.EQUALS));
        return this.readServers(query);
    }

    public @NotNull Set<SmartServer> serversByProvider(@NotNull String providerId) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        ReadRequest query = db.newReadRequest(TABLE);
        query.withFilter(Filter.by("provider_id", providerId, Filter.EQUALS));
        return this.readServers(query);
    }

    public int countByProvider(@NotNull String providerId) throws Exception {
        return serversByProvider(providerId).size();
    }

    public @NotNull Optional<SmartServer> fetch(@NotNull String id) throws Exception {
        HazeDatabase db = this.database.get(15, TimeUnit.SECONDS);
        ReadRequest query = db.newReadRequest(TABLE);
        query.withFilter(Filter.by("id", id, Filter.EQUALS));
        return this.readServers(query).stream().findAny();
    }

    private Set<SmartServer> readServers(ReadRequest query) throws Exception {
        Set<SmartServer> servers = new HashSet<>();
        query.executeAndFilter(SmartServer.class, row -> {
            servers.add(new SmartServer(
                    row.getString("id"),
                    row.getString("provider_id"),
                    row.getString("family_id"),
                    row.getString("rc_server_id"),
                    row.getString("status"),
                    row.<Number>get("ram_amount").longValue(),
                    this.createdAt(row)
            ));
            return false;
        });
        return servers;
    }

    private Instant createdAt(DataEntry row) {
        return switch (row.<Object>get("created_at")) {
            case Instant instant -> instant;
            case LocalDateTime dateTime -> dateTime.toInstant(ZoneOffset.UTC);
            case Timestamp timestamp -> timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
            case null -> throw new IllegalStateException("Missing created_at for Smart server " + row.getString("id"));
            default -> throw new IllegalStateException("Unsupported created_at value for Smart server " + row.getString("id"));
        };
    }
}
