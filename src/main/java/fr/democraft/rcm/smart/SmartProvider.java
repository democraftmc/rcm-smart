package fr.democraft.rcm.smart;
import fr.democraft.rcm.smart.config.MainConfig;
import fr.democraft.rcm.smart.config.Provider;
import fr.democraft.rcm.smart.config.ProviderConfig;
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
    public static boolean DEBUG = false;
    public static final boolean useFancyLogger = true;
    public static SmartLogger logger; // Static loggers instance
    public static MainConfig config;
    public static ProviderConfig providerConfig;

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
            try {
                config = MainConfig.New();
                providerConfig = ProviderConfig.New();
                DEBUG = config.debug;
            } catch (Exception e) {
                System.out.println("[SmartRC] Failed to load configuration files.");
                e.printStackTrace();
                return;
            }
            if (config.fancyLogs) { SmartProvider.logger = new FancyLogger();
            } else { SmartProvider.logger = new ClassicLogger();}
            logger.log("Rusty's Smart Provider is booting up, please wait...");

            // Register Cap Events (which trigger the RCS Events)
            kernel.<EventManager>fetchModule("EventManager").onStart(m -> {
                m.listen(OnServerPreJoin.class);
                m.listen(OnServerLeave.class);
            });

            if (DEBUG) {
                for (Provider provider : providerConfig.providers) {
                    logger.debug("Loaded provider: " + provider.id);
                }
            }

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
