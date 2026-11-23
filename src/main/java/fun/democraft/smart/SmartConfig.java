package fun.democraft.smart;

import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;

import java.util.Map;

@Namespace("rustyconnector")
@Config("/smart/config.yml")
@Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                         Smart                            #",
        "#                                                          #",
        "# | Smart is the orchestration core responsible for       #",
        "# | provisioning and deprovisioning servers through        #",
        "# | registered providers (pterodactyl, etc.).              #",
        "# |                                                          #",
        "# | Providers themselves are configured in                 #",
        "# | rustyconnector/smart/providers/<id>.yml                #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################"
})
public class SmartConfig {
    @Comment({
            "#",
            "# The name of the Haze database used to persist the",
            "# registry of live smart servers. Make sure a Haze",
            "# driver (MySQL, Postgres, ...) is installed and",
            "# the database is registered under this name.",
            "#"
    })
    @Node(0)
    public String database = "default";

    @Comment({
            "#",
            "# Startup targets by family ID, for example {lobby: 3, survival: 2}.",
            "# Smart creates enough servers to reach each target, counting",
            "# existing ONLINE and PROVISIONING servers in the registry.",
            "# Use an empty map or a target of zero to disable provisioning.",
            "#"
    })
    @Node(value = 1, key = "startup-servers")
    public Map<String, Integer> startupServers = Map.of();

    public static SmartConfig New() {
        return DeclarativeYAML.From(SmartConfig.class);
    }
}
