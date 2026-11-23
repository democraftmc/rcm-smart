package fun.democraft.smart;

import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.lib.Printer;

import java.util.List;
import java.util.Map;

@Namespace("rustyconnector")
@Config("/smart/providers/{id}.yml")
@Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                      Smart Provider                      #",
        "#                                                          #",
        "# | A provider describes which server families it can     #",
        "# | serve and under which process constraints.             #",
        "#                                                          #",
        "# | Per-server resources (RAM, disk, egg, ...) are NOT     #",
        "# | configured here — they live in each family's metadata  #",
        "# | under the \"smart\" key, e.g.:                          #",
        "# |   { \"smart\": { \"memory\": 2048,                       #",
        "# |       \"pterodactyl\": { \"egg\": 123, ... } } }           #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################"
})
public class ProviderConfig {
    @PathParameter("id")
    public String id;

    @Comment({
            "#",
            "# Unique id of this provider instance. Must match the file name",
            "# and the id the provider module registers itself with.",
            "#"
    })
    @Node(0)
    public String providerId = "";

    @Comment({
            "#",
            "# Families this provider is allowed to serve.",
            "# Leave empty to allow all families (minus excluded ones).",
            "#"
    })
    @Node(1)
    public List<String> includedFamilies = List.of();

    @Comment({
            "#",
            "# Families this provider must never serve.",
            "# Excluded families always win over included ones.",
            "#"
    })
    @Node(2)
    public List<String> excludedFamilies = List.of();

    @Comment({
            "#",
            "# Maximum amount of server processes this provider",
            "# is allowed to run at the same time.",
            "#"
    })
    @Node(3)
    public int maxProcesses = 5;

    @Comment({
            "#",
            "# Priority of this provider. When multiple providers can serve",
            "# the same family, the one with the highest priority is tested first.",
            "# Register the same provider type multiple times (different file",
            "# names / ids) to scale across regions or panels.",
            "#"
    })
    @Node(4)
    public int priority = 0;

    public static ProviderConfig New(String providerId) {
        Printer printer = new Printer()
                .pathReplacements(Map.of("id", providerId))
                .commentReplacements(Map.of("id", providerId));
        return DeclarativeYAML.From(ProviderConfig.class, printer);
    }
}
