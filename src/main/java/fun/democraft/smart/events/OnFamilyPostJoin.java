package fun.democraft.smart.events;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.common.events.EventPriority;
import fun.democraft.smart.SmartModule;
import group.aelysium.rustyconnector.proxy.events.FamilyPostJoinEvent;

/**
 * Example hook: when a player joins a family that has no available server,
 * ask Smart to provision one through a matching provider.
 */
public class OnFamilyPostJoin {
    private final SmartModule smart;

    public OnFamilyPostJoin(SmartModule smart) {
        this.smart = smart;
    }

    @EventListener(order = EventPriority.LOWEST)
    public void handle(FamilyPostJoinEvent event) {
        try {
            this.smart.provisionIfEmpty(event.family);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To handle FamilyPostJoinEvent in Smart."));
        }
    }
}
