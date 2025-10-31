package fr.democraft.rcm.smart.listeners;

import fr.democraft.rcm.smart.SmartProvider;
import fr.democraft.rcm.smart.config.MainConfig;
import fr.democraft.rcm.smart.events.CreatePhysicalServer;
import fr.democraft.rcm.smart.events.DeletePhysicalServer;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.ServerLeaveEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OnServerLeave {
    @EventListener
    public static void handler(ServerLeaveEvent event) throws ExecutionException, InterruptedException, TimeoutException {
        Server s = event.server;
        if (s.players() == (s.softPlayerCap() + SmartProvider.config.playerCap)) {
            Optional<Family> optionalSmartFamily = s.family();
            if (optionalSmartFamily.isPresent()) {
                Family smartFamily = optionalSmartFamily.get();
                // Magic things that call the event/abstract creator.
                // For now, no ram/managment logic, just calling this event
                // As proof of concept.

                //                                         This guy here is problematic ⬇
                DeletePhysicalServer subEvent = new DeletePhysicalServer("pterodactyl", smartFamily); // Build a new instance of your custom event.
                boolean status = RC.EventManager().fireEvent(subEvent).get(10, TimeUnit.SECONDS);
                if (!status) {
                    SmartProvider.logger.error("No providers have deleted a" + smartFamily.displayName() + " server!");
                }
            }
        }
    }
}
