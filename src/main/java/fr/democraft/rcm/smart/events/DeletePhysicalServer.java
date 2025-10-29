package fr.democraft.rcm.smart.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

public class DeletePhysicalServer extends Event.Cancelable {
    public final String providerId;
    public final Family family;

    public DeletePhysicalServer(@NotNull String providerId, @NotNull Family family) {
        super();
        this.providerId = providerId;
        this.family = family;
    }
}
