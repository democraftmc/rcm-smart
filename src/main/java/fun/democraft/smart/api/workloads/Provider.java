package fun.democraft.smart.api.workloads;

import group.aelysium.rustyconnector.RC;
import fun.democraft.smart.ProviderConfig;
import fun.democraft.smart.SmartModule;
import fun.democraft.smart.api.SmartProvider;

import java.io.IOException;
import java.util.Set;

public abstract class Provider implements SmartProvider, AutoCloseable {
    private final ProviderConfig config;

    protected final Api api;

    protected Provider(ProviderConfig config, Api api) {
        this.config = config;
        this.api = api;
    }

    @FunctionalInterface
    protected interface Step { void run() throws Exception; }

    protected String create(String path, Object payload, String reference, String... idFields) throws Exception {
        try {
            return Api.responseId(api.call("POST", path, payload, false), idFields);
        } catch (IOException failure) {
            // A lost reply can leave a workload behind. Never reissue the create request.
            throw new IOException("Create reference " + reference + ": " + failure.getMessage());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IOException("Create interrupted for reference " + reference, failure);
        }
    }

    protected String finish(String id, Step step) throws Exception {
        try {
            step.run();
            return id;
        } catch (Exception failure) {
            try {
                deprovision(id);
            } catch (Exception cleanup) {
                var unresolved = new IOException("Could not clean up provider resource " + id
                        + " after provisioning failed: " + failure.getMessage(), failure);
                unresolved.addSuppressed(cleanup);
                if (cleanup instanceof InterruptedException) Thread.currentThread().interrupt();
                throw unresolved;
            } finally {
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            }
            throw failure;
        }
    }

    @Override
    public String id() {
        return this.config.providerId;
    }

    @Override
    public Set<String> includedFamilies() {
        return Set.copyOf(this.config.includedFamilies);
    }

    @Override
    public Set<String> excludedFamilies() {
        return Set.copyOf(this.config.excludedFamilies);
    }

    @Override
    public int maxProcesses() {
        return this.config.maxProcesses;
    }

    @Override
    public int priority() {
        return this.config.priority;
    }

    @Override
    public int runningProcesses() {
        // Ask the Smart core — the smart_servers table is the source of truth,
        // no local cache needed.
        try {
            return smart().servers().countByProvider(id());
        } catch (Exception e) {
            // Treat unavailable accounting as full capacity.
            return maxProcesses();
        }
    }

    @Override
    public void close() {
        api.close();
    }

    private SmartModule smart() {
        return (SmartModule) RC.Kernel().fetchModule("Smart").orElseThrow();
    }
}
