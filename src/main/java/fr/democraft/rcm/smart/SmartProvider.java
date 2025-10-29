package fr.democraft.rcm.smart;

import fr.democraft.rcm.smart.listeners.OnServerPreJoin;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmartProvider implements Module {
    @Override
    public @Nullable Component details() {
        return Component.text("Provides smart server management features.");
    }

    @Override
    public void close() throws Exception {}

    public static class Builder extends ExternalModuleBuilder<SmartProvider> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull SmartProvider instance) {
            kernel.<EventManager>fetchModule("EventManager").onStart(m -> {
                m.listen(OnServerPreJoin.class);
            });
        }
        
        @NotNull
        @Override
        public SmartProvider onStart(@NotNull Context context) throws Exception {
            return new SmartProvider();
        }
    }
}