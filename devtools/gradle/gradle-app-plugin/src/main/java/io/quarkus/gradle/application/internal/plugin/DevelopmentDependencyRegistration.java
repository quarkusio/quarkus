package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import io.quarkus.gradle.tooling.ToolingUtils;

final class DevelopmentDependencyRegistration {

    private DevelopmentDependencyRegistration() {
    }

    static Configuration register(Project project, boolean legacyPluginPresent) {
        if (legacyPluginPresent) {
            return project.getConfigurations().getByName(ToolingUtils.DEV_MODE_CONFIGURATION_NAME);
        }
        if (project.getConfigurations().findByName(ToolingUtils.DEV_MODE_CONFIGURATION_NAME) != null) {
            throw new GradleException("Cannot create the standalone Quarkus development dependency configuration '"
                    + ToolingUtils.DEV_MODE_CONFIGURATION_NAME
                    + "' because a configuration with that name already exists and the legacy 'io.quarkus' plugin "
                    + "does not own it.");
        }
        return project.getConfigurations().dependencyScope(ToolingUtils.DEV_MODE_CONFIGURATION_NAME,
                configuration -> configuration
                        .setDescription("Declares dependencies used only for Quarkus development."))
                .get();
    }
}
