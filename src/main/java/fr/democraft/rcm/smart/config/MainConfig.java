package fr.democraft.rcm.smart.config;

import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.Config;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.Namespace;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.Node;

@Namespace("rustyconnector-modules")
@Config("/smart/config.yml")
public class MainConfig {
    @Node(1)
    public boolean debug = true;

    @Node(2)
    public boolean fancyLogs = true;

    public static MainConfig New() {
        return DeclarativeYAML.From(MainConfig.class);
    }
}
