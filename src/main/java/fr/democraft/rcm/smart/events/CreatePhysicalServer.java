package fr.democraft.rcm.smart.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

public class CreatePhysicalServer extends Event {
    public final Family family;

    public CreatePhysicalServer(@NotNull Family family) {
        super();
        this.family = family;
    }
}
