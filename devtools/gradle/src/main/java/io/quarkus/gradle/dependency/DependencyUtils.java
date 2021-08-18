package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Category;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;

public class DependencyUtils {

    private static final String COPY_CONFIGURATION_NAME = "quarkusDependency";
    private static final String TEST_FIXTURE_SUFFIX = "-test-fixtures";

    public static Configuration duplicateConfiguration(Project project, Configuration... toDuplicates) {
        Configuration configurationCopy = project.getConfigurations().findByName(COPY_CONFIGURATION_NAME);
        if (configurationCopy != null) {
            project.getConfigurations().remove(configurationCopy);
        }
        configurationCopy = project.getConfigurations().create(COPY_CONFIGURATION_NAME);

        // We add boms for dependency resolution
        List<Dependency> boms = getEnforcedPlatforms(project);
        configurationCopy.getDependencies().addAll(boms);

        for (Configuration toDuplicate : toDuplicates) {
            for (Dependency dependency : toDuplicate.getAllDependencies()) {
                if (includedBuild(project, dependency.getName()) != null) {
                    continue;
                }
                if (isTestFixtureDependency(dependency)) {
                    continue;
                }
                configurationCopy.getDependencies().add(dependency);
            }
        }
        return configurationCopy;
    }

    public static Dependency create(DependencyHandler dependencies, String conditionalDependency) {
        AppArtifactCoords dependencyCoords = AppArtifactCoords.fromString(conditionalDependency);
        return dependencies.create(String.join(":", dependencyCoords.getGroupId(), dependencyCoords.getArtifactId(),
                dependencyCoords.getVersion()));
    }

    public static boolean exist(Set<ResolvedArtifact> runtimeArtifacts, List<AppArtifactKey> dependencies) {
        final Set<AppArtifactKey> rtKeys = new HashSet<>(runtimeArtifacts.size());
        runtimeArtifacts.forEach(r -> rtKeys.add(
                new AppArtifactKey(r.getModuleVersion().getId().getGroup(), r.getName(), r.getClassifier(), r.getExtension())));
        return rtKeys.containsAll(dependencies);
    }

    public static boolean exists(Set<ResolvedArtifact> runtimeArtifacts, Dependency dependency) {
        for (ResolvedArtifact runtimeArtifact : runtimeArtifacts) {
            ModuleVersionIdentifier artifactId = runtimeArtifact.getModuleVersion().getId();
            if (artifactId.getGroup().equals(dependency.getGroup()) && artifactId.getName().equals(dependency.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTestFixtureDependency(Dependency dependency) {
        if (!(dependency instanceof ModuleDependency)) {
            return false;
        }
        ModuleDependency module = (ModuleDependency) dependency;
        for (Capability requestedCapability : module.getRequestedCapabilities()) {
            if (requestedCapability.getName().endsWith(TEST_FIXTURE_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    public static List<Dependency> getEnforcedPlatforms(Project project) {
        final List<org.gradle.api.artifacts.Dependency> directExtension = new ArrayList<>();
        final Configuration impl = project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);

        for (Dependency d : impl.getAllDependencies()) {
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

    public static String asDependencyNotation(Dependency dependency) {
        return String.join(":", dependency.getGroup(), dependency.getName(), dependency.getVersion());
    }

    public static String asDependencyNotation(AppArtifactCoords artifactCoords) {
        return String.join(":", artifactCoords.getGroupId(), artifactCoords.getArtifactId(), artifactCoords.getVersion());
    }

    public static String asCapabilityNotation(AppArtifactCoords artifactCoords) {
        return String.join(":", artifactCoords.getGroupId(), artifactCoords.getArtifactId() + "-capability",
                artifactCoords.getVersion());
    }

    public static String asFeatureName(ModuleVersionIdentifier version) {
        return version.getGroup() + ":" + version.getName();
    }

    public static String asFeatureName(Dependency version) {
        return version.getGroup() + ":" + version.getName();
    }
}
