package fr.democraft.rcm.smart;

import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.events.ServerPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SmartProvider implements Module {
    @Override
    public @Nullable Component details() {
        return null;
    }

    @Override
    public void close() throws Exception {}

    public static class Builder extends ExternalModuleBuilder<SmartProvider> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull SmartProvider instance) {
            try {
                kernel.fetchModule("EventManager").onStart(e -> {
                    EventManager eventManager = (EventManager) e;
                    eventManager.listen(ServerPreJoinEvent.class);

                    System.out.println("Smart module registered with EventManager!");
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        @NotNull
        @Override
        public SmartProvider onStart(@NotNull Context context) throws Exception {
            return new SmartProvider();
        }
    }

    @EventListener
    public static void handler(ServerPreJoinEvent event) {
        Server s = event.server;
        if (s.softPlayerCap() == s.players()) {
            Optional<Family> smartFamily = s.family();
            if (smartFamily.isPresent()) {
                // Magic things that call the event/abstract creator.
                // For now, no ram/managment logic, just calling this event
                // As proof of concept.
            }
        }
    }
}