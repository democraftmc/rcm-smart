package fr.democraft.rcm.smart;
import fr.democraft.rcm.smart.listeners.OnServerLeave;
import fr.democraft.rcm.smart.listeners.OnServerPreJoin;
import fr.democraft.rcm.smart.loggers.SmartLogger;
import fr.democraft.rcm.smart.loggers.ClassicLogger;
import fr.democraft.rcm.smart.loggers.FancyLogger;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmartProvider implements Module {
    public static final boolean DEBUG = false;
    public static final boolean useFancyLogger = true;
    public static SmartLogger logger; // Static loggers instance

    @Override
    public @Nullable Component details() {
        return Component.text("Provides smart server management features.");
    }

    @Override
    public void close() throws Exception {}

    public static class Builder extends ExternalModuleBuilder<SmartProvider> {
        @Override
        public void bind(@NotNull ProxyKernel kernel, @NotNull SmartProvider instance) {
            // Initialize the loggers
            if (useFancyLogger) { SmartProvider.logger = new FancyLogger();
            } else { SmartProvider.logger = new ClassicLogger();}
            logger.log("Rusty's Smart Provider is booting up, please wait...");

            // Register Cap Events (which trigger the RCS Events)
            kernel.<EventManager>fetchModule("EventManager").onStart(m -> {
                m.listen(OnServerPreJoin.class);
                m.listen(OnServerLeave.class);
            });
            logger.debug("Event registered successfully");
            logger.log("Module loaded successfully");
        }

        @NotNull
        @Override
        public SmartProvider onStart(@NotNull Context context) throws Exception {
            return new SmartProvider();
        }
    }
}
