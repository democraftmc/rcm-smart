package fun.democraft.smart;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import fun.democraft.smart.events.OnFamilyPostJoin;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import org.jetbrains.annotations.NotNull;

public class SmartBuilder extends ExternalModuleBuilder<SmartModule> {
    @NotNull
    @Override
    public SmartModule onStart(@NotNull Context context) throws Exception {
        SmartConfig config = SmartConfig.New();
        Flux<HazeDatabase> database = RC.P.Haze().fetchDatabase(config.database);
        SmartModule module = new SmartModule(config, database);
        module.servers().init();
        return module;
    }

    @Override
    public void bind(@NotNull ProxyKernel kernel, @NotNull SmartModule instance) {
        try {
            kernel.<EventManager>fetchModule("EventManager").onStart(events -> {
                events.listen(new OnFamilyPostJoin(instance));
                instance.startStartupProvisioning(events);
            });
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To bind Smart to ProxyKernel."));
        }
    }
}
