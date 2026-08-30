package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.QuarkusApplicationPlugin.EXTENSION_NAME;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import io.quarkus.gradle.GradleVersionSupport;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.gradle.application.internal.tooling.GradleApplicationModelBuilder;
import io.quarkus.gradle.application.internal.tooling.GradleApplicationModelSidecarBuilder;

public abstract class PluginInternal implements Plugin<Project> {
    private static final String LEGACY_PLUGIN_ID = "io.quarkus";

    private final ToolingModelBuilderRegistry toolingModels;
    private final BuildEventsListenerRegistry buildEventsListeners;

    protected PluginInternal(ToolingModelBuilderRegistry toolingModels,
            BuildEventsListenerRegistry buildEventsListeners) {
        this.toolingModels = toolingModels;
        this.buildEventsListeners = buildEventsListeners;
    }

    @Override
    public void apply(Project project) {
        GradleVersionSupport.requireMinimumGradleVersion();

        DslLifecycleCoordinator lifecycle = new DslLifecycleCoordinator();
        boolean legacyPluginPresent = project.getPlugins().hasPlugin(LEGACY_PLUGIN_ID);
        project.getPluginManager().apply(JavaPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().create(EXTENSION_NAME,
                QuarkusApplicationExtension.class, project.getObjects(), project.getProviders(), project.getLayout(),
                project.getName(), project.provider(() -> project.getVersion().toString()), lifecycle);
        ApplicationModelResolutionViews resolutionViews = new TaskRegistration()
                .register(project, extension, legacyPluginPresent, buildEventsListeners, lifecycle);
        registerToolingModelsIfOwned(legacyPluginPresent, toolingModels, resolutionViews);
        project.getPlugins().withId(LEGACY_PLUGIN_ID, ignored -> project.getLogger().warn(
                "Both 'io.quarkus.application' and legacy 'io.quarkus' are applied to this project. "
                        + "This is supported as migration mode only when legacy 'io.quarkus' is applied first. "
                        + "Legacy owns Gradle Test task instrumentation in migration mode, but legacy tasks do not "
                        + "inherit the new plugin's Gradle configuration-cache and isolated-project compatibility guarantees."));
    }

    static void registerToolingModelsIfOwned(boolean legacyPluginPresent, ToolingModelBuilderRegistry toolingModels,
            ApplicationModelResolutionViews resolutionViews) {
        if (legacyPluginPresent) {
            return;
        }
        GradleApplicationModelBuilder applicationModelBuilder = new GradleApplicationModelBuilder(resolutionViews);
        toolingModels.register(applicationModelBuilder);
        toolingModels.register(new GradleApplicationModelSidecarBuilder(resolutionViews,
                applicationModelBuilder::build));
    }
}
