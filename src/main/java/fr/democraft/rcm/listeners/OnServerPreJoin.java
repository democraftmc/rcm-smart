package fr.democraft.rcm.listeners;

import fr.democraft.rcm.events.CreatePhysicalServer;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.ServerPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.Optional;

public class OnServerPreJoin {
    @EventListener
    public static void handler(ServerPreJoinEvent event) {
        Server s = event.server;
        if (s.softPlayerCap() == s.players()) {
            Optional<Family> optionalSmartFamily = s.family();
            if (optionalSmartFamily.isPresent()) {
                Family smartFamily = optionalSmartFamily.get();
                // Magic things that call the event/abstract creator.
                // For now, no ram/managment logic, just calling this event
                // As proof of concept.
                CreatePhysicalServer subEvent = new CreatePhysicalServer(smartFamily); // Build a new instance of your custom event.
                RC.EventManager().fireEvent(subEvent); // Fire the event.
            }
        }
    }
}
