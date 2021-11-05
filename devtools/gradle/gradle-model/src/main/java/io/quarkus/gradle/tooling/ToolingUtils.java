package io.quarkus.gradle.tooling;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.plugins.JavaPlugin;

public class ToolingUtils {

    private static final String DEPLOYMENT_CONFIGURATION_SUFFIX = "Deployment";
    public static final String DEV_MODE_CONFIGURATION_NAME = "quarkusDev";

    public static String toDeploymentConfigurationName(String baseConfigurationName) {
        return baseConfigurationName + DEPLOYMENT_CONFIGURATION_SUFFIX;
    }

    public static List<Dependency> getEnforcedPlatforms(Project project) {
        return getEnforcedPlatforms(project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME));
    }

    public static List<Dependency> getEnforcedPlatforms(Configuration config) {
        final List<org.gradle.api.artifacts.Dependency> directExtension = new ArrayList<>();
        for (Dependency d : config.getAllDependencies()) {
            if (!(d instanceof ModuleDependency)) {
                continue;
            }
            final ModuleDependency module = (ModuleDependency) d;
            if (isEnforcedPlatform(module)) {
                directExtension.add(d);
            }
        }
        return directExtension;
    }

    public static boolean isEnforcedPlatform(ModuleDependency module) {
        final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
        return category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                || Category.REGULAR_PLATFORM.equals(category.getName()));
    }

    public static IncludedBuild includedBuild(final Project project, final String projectName) {
        try {
            return project.getGradle().includedBuild(projectName);
        } catch (UnknownDomainObjectException ignore) {
            return null;
        }
    }

}
