package io.quarkus.gradle;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.model.RemoteToolingModelBuilder;
import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateConfig;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusNative;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusPlugin implements Plugin<Project> {

    private final ToolingModelBuilderRegistry registry;

    @Inject
    public QuarkusPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        verifyGradleVersion();
        // register extension
        project.getExtensions().create("quarkus", QuarkusPluginExtension.class, project);

        registerTasks(project);
        registry.register(new RemoteToolingModelBuilder());
    }

    private void registerTasks(Project project) {
        TaskContainer tasks = project.getTasks();
        tasks.create("listExtensions", QuarkusListExtensions.class);
        tasks.create("addExtension", QuarkusAddExtension.class);

        Task quarkusBuild = tasks.create("quarkusBuild", QuarkusBuild.class);
        Task quarkusGenerateConfig = tasks.create("generateConfig", QuarkusGenerateConfig.class);
        Task quarkusDev = tasks.create("quarkusDev", QuarkusDev.class);

        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(quarkusBuild));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    Task classesTask = tasks.getByName(JavaPlugin.CLASSES_TASK_NAME);
                    quarkusDev.dependsOn(classesTask);
                    quarkusBuild.dependsOn(classesTask);
                });

        tasks.create("buildNative", QuarkusNative.class).dependsOn(quarkusBuild);
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 5.0 or later. Current version is: " +
                    GradleVersion.current());
        }
    }
}
