package fr.democraft.rcm.helloworld;

import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HelloWorld implements Module {
    @Override
    public @Nullable Component details() {
        return null;
    }

    @Override
    public void close() throws Exception {}

    public static class Builder extends ExternalModuleBuilder<HelloWorld> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull HelloWorld instance) {
            try {
                kernel.fetchModule("EventManager").onStart(e -> {
                    EventManager eventManager = (EventManager) e;
                    eventManager.listen(ServerRegisterEvent.class);

                    System.out.println("HelloWorld module registered with EventManager!");
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        @NotNull
        @Override
        public HelloWorld onStart(@NotNull Context context) throws Exception {
            System.out.println("Hello World from RustyConnector Module!");
            return new HelloWorld();
        }
    }

    @EventListener
    public static void handler(ServerRegisterEvent event) {
        System.out.println(event.server.address() + " has registered to the family "+ event.family.displayName());
    }
}