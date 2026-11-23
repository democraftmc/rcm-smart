package group.aelysium.rustyconnector.smart.packaging;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public final class SmartProviderPackagingPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("java", plugin -> {
            var generate = project.getTasks().register(
                    "generateSmartProviderVelocityResources", GenerateVelocityResources.class, task -> {
                        task.getPluginId().set(project.getName());
                        task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                                .dir("generated/resources/smartProviderVelocity"));
                    });
            project.afterEvaluate(evaluatedProject ->
                    generate.configure(task -> task.getPluginVersion().set(evaluatedProject.getVersion().toString())));

            var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME, sourceSet ->
                    sourceSet.getResources().srcDir(generate.flatMap(GenerateVelocityResources::getOutputDirectory)));
        });
    }
}
