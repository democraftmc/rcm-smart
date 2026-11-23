package group.aelysium.rustyconnector.smart.packaging;

import groovy.json.JsonOutput;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@DisableCachingByDefault(because = "Generating a small descriptor and copying one class is cheaper than caching.")
public abstract class GenerateVelocityResources extends DefaultTask {
    private static final String ENTRYPOINT = "group.aelysium.rustyconnector.smart.velocity.MisplacedProviderPlugin";
    private static final String ENTRYPOINT_RESOURCE = ENTRYPOINT.replace('.', '/') + ".class";

    @Input
    public abstract Property<String> getPluginId();

    @Input
    public abstract Property<String> getPluginVersion();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() throws IOException {
        String pluginId = getPluginId().get();
        if (!pluginId.matches("[a-z][a-z0-9_-]{0,63}")) {
            throw new GradleException("Project name must be a valid Velocity plugin ID: " + pluginId);
        }

        var output = getOutputDirectory().get().getAsFile().toPath();
        var entrypoint = output.resolve(ENTRYPOINT_RESOURCE);
        Files.createDirectories(entrypoint.getParent());
        try (var input = GenerateVelocityResources.class.getResourceAsStream("/" + ENTRYPOINT_RESOURCE)) {
            if (input == null) {
                throw new GradleException("Missing bundled Velocity entrypoint: " + ENTRYPOINT_RESOURCE);
            }
            Files.write(entrypoint, input.readAllBytes());
        }

        String descriptor = JsonOutput.toJson(Map.of(
                "id", pluginId,
                "main", ENTRYPOINT,
                "name", pluginId,
                "version", getPluginVersion().get(),
                "description", "RustyConnector Smart provider. Install in rc-modules/."));
        Files.writeString(output.resolve("velocity-plugin.json"), JsonOutput.prettyPrint(descriptor) + "\n");
    }
}
