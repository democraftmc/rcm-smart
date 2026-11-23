package fun.democraft.smart;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.FamilyRegisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class StartupProvisioning implements AutoCloseable {
    private final SmartModule smart;
    private final Map<String, Integer> pending = new ConcurrentSkipListMap<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            1, Thread.ofPlatform().daemon().name("smart-startup-provisioning").factory());
    private ScheduledFuture<?> scheduled;

    StartupProvisioning(SmartModule smart) {
        this.smart = smart;
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        smart.config().startupServers.forEach((familyId, target) -> {
            if (target < 0) {
                RC.Error(Error.from("Smart startup target for " + familyId + " must be zero or greater.").urgent(true));
            } else if (target > 0) {
                this.pending.put(familyId, target);
            }
        });
    }

    @EventListener
    public void onFamilyRegister(FamilyRegisterEvent event) {
        if (this.pending.keySet().stream().anyMatch(event.family.id()::equalsIgnoreCase)) {
            this.schedule();
        }
    }

    synchronized void schedule() {
        if (this.executor.isShutdown() || this.pending.isEmpty()) return;
        if (this.scheduled != null) this.scheduled.cancel(false);
        this.scheduled = this.executor.schedule(this::provisionPendingFamilies, 1, TimeUnit.SECONDS);
    }

    private void provisionPendingFamilies() {
        for (var entry : this.pending.entrySet()) {
            if (!this.smart.isActive() || Thread.currentThread().isInterrupted()) return;

            String familyId = entry.getKey();
            try {
                Flux<Family> registeredFamily = RC.P.Families().find(familyId);
                Family family = registeredFamily == null ? null : registeredFamily.orElse(null);
                if (family == null) {
                    RC.Error(Error.from("Smart startup family " + familyId + " is not registered.").urgent(true));
                    continue;
                }

                if (this.smart.fillStartupTarget(family, entry.getValue())) {
                    this.pending.remove(familyId);
                }
            } catch (Exception e) {
                this.pending.remove(familyId);
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                RC.Error(Error.from(e)
                        .whileAttempting("To fill the Smart startup target for " + familyId).urgent(true));
            }
        }
    }

    @Override
    public synchronized void close() {
        if (this.scheduled != null) this.scheduled.cancel(false);
        this.executor.shutdown();
        this.pending.clear();
    }
}
