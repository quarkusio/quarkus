package io.quarkus.gradle.dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.gradle.tooling.dependency.LocalExtensionDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

public class ApplicationDeploymentClasspathBuilder {

    private static String getRuntimeConfigName(LaunchMode mode, boolean base) {
        final StringBuilder sb = new StringBuilder();
        sb.append("quarkus");
        if (mode == LaunchMode.DEVELOPMENT) {
            sb.append("Dev");
        } else if (mode == LaunchMode.TEST) {
            sb.append("Test");
        } else {
            sb.append("Prod");
        }
        if (base) {
            sb.append("Base");
        }
        sb.append("RuntimeClasspathConfiguration");
        return sb.toString();
    }

    public static String getBaseRuntimeConfigName(LaunchMode mode) {
        return getRuntimeConfigName(mode, true);
    }

    public static String getFinalRuntimeConfigName(LaunchMode mode) {
        return getRuntimeConfigName(mode, false);
    }

    public static void initConfigurations(Project project) {
        final ConfigurationContainer configContainer = project.getConfigurations();

        // Custom configuration for dev mode
        configContainer.create(ToolingUtils.DEV_MODE_CONFIGURATION_NAME)
                .extendsFrom(configContainer.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME));

        // Base runtime configurations for every launch mode
        configContainer.create(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.TEST))
                .extendsFrom(configContainer.getByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME));

        configContainer.create(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.NORMAL))
                .extendsFrom(configContainer.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));

        configContainer.create(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.DEVELOPMENT))
                .extendsFrom(
                        configContainer.getByName(ToolingUtils.DEV_MODE_CONFIGURATION_NAME),
                        configContainer.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME),
                        configContainer.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));

        // enable the Panache annotation processor on the classpath, if it's found among the dependencies
        configContainer.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .withDependencies(annotationProcessors -> {
                    Set<ResolvedArtifact> compileClasspathArtifacts = DependencyUtils
                            .duplicateConfiguration(project, configContainer
                                    .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME))
                            .getResolvedConfiguration()
                            .getResolvedArtifacts();
                    for (ResolvedArtifact artifact : compileClasspathArtifacts) {
                        if ("quarkus-panache-common".equals(artifact.getName())
                                && "io.quarkus".equals(artifact.getModuleVersion().getId().getGroup())) {
                            project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                                    "io.quarkus:quarkus-panache-common:" + artifact.getModuleVersion().getId().getVersion());
                        }
                    }
                });
    }

    private final Project project;
    private final Collection<org.gradle.api.artifacts.Dependency> enforcedPlatforms;
    private final LaunchMode mode;
    private ConditionalDependenciesEnabler cdEnabler;
    private Configuration runtimeConfig;

    public ApplicationDeploymentClasspathBuilder(Project project, LaunchMode mode,
            Collection<org.gradle.api.artifacts.Dependency> enforcedPlatforms) {
        this.project = project;
        this.mode = mode;
        this.enforcedPlatforms = enforcedPlatforms;
    }

    public Configuration getRuntimeConfiguration() {
        if (runtimeConfig == null) {
            final String configName = getFinalRuntimeConfigName(mode);
            runtimeConfig = project.getConfigurations().findByName(configName);
            if (runtimeConfig == null) {
                runtimeConfig = DependencyUtils.duplicateConfiguration(project, configName,
                        getConditionalDependenciesEnabler().getBaseRuntimeConfiguration());
            }
        }
        return runtimeConfig;
    }

    public Configuration getDeploymentConfiguration() {
        String deploymentConfigurationName = ToolingUtils.toDeploymentConfigurationName(runtimeConfig.getName());
        Configuration deploymentConfig = project.getConfigurations().findByName(deploymentConfigurationName);
        if (deploymentConfig != null) {
            return deploymentConfig;
        }

        final Collection<ExtensionDependency> allExtensions = getConditionalDependenciesEnabler().getAllExtensions();
        Set<ExtensionDependency> extensions = collectFirstMetQuarkusExtensions(getRuntimeConfiguration(), allExtensions);
        // Add conditional extensions
        for (ExtensionDependency knownExtension : allExtensions) {
            if (knownExtension.isConditional()) {
                extensions.add(knownExtension);
            }
        }

        return project.getConfigurations().create(deploymentConfigurationName, config -> {
            config.withDependencies(ds -> ds.addAll(enforcedPlatforms));

            final Set<ModuleVersionIdentifier> alreadyProcessed = new HashSet<>(extensions.size());
            final DependencyHandler dependencies = project.getDependencies();
            for (ExtensionDependency extension : extensions) {
                if (extension instanceof LocalExtensionDependency) {
                    DependencyUtils.addLocalDeploymentDependency(deploymentConfigurationName,
                            (LocalExtensionDependency) extension, dependencies);
                } else {
                    if (!alreadyProcessed.add(extension.getExtensionId())) {
                        continue;
                    }
                    DependencyUtils.requireDeploymentDependency(deploymentConfigurationName, extension, dependencies);
                }
            }
        });
    }

    private ConditionalDependenciesEnabler getConditionalDependenciesEnabler() {
        if (cdEnabler == null) {
            cdEnabler = new ConditionalDependenciesEnabler(project, mode, enforcedPlatforms);
        }
        return cdEnabler;
    }

    private Set<ExtensionDependency> collectFirstMetQuarkusExtensions(Configuration configuration,
            Collection<ExtensionDependency> knownExtensions) {

        Set<ExtensionDependency> firstLevelExtensions = new HashSet<>();
        Set<ResolvedDependency> firstLevelModuleDependencies = configuration.getResolvedConfiguration()
                .getFirstLevelModuleDependencies();

        Set<String> visitedArtifacts = new HashSet<>();
        for (ResolvedDependency firstLevelModuleDependency : firstLevelModuleDependencies) {
            firstLevelExtensions
                    .addAll(collectQuarkusExtensions(firstLevelModuleDependency, visitedArtifacts, knownExtensions));
        }
        return firstLevelExtensions;
    }

    private Set<ExtensionDependency> collectQuarkusExtensions(ResolvedDependency dependency, Set<String> visitedArtifacts,
            Collection<ExtensionDependency> knownExtensions) {
        String artifactKey = String.format("%s:%s", dependency.getModuleGroup(), dependency.getModuleName());
        if (!visitedArtifacts.add(artifactKey)) {
            return Collections.emptySet();
        }

        Set<ExtensionDependency> extensions = new LinkedHashSet<>();
        ExtensionDependency extension = getExtensionOrNull(dependency.getModuleGroup(), dependency.getModuleName(),
                dependency.getModuleVersion(), knownExtensions);
        if (extension != null) {
            extensions.add(extension);
        } else {
            for (ResolvedDependency child : dependency.getChildren()) {
                extensions.addAll(collectQuarkusExtensions(child, visitedArtifacts, knownExtensions));
            }
        }
        return extensions;
    }

    private ExtensionDependency getExtensionOrNull(String group, String artifact, String version,
            Collection<ExtensionDependency> knownExtensions) {
        for (ExtensionDependency knownExtension : knownExtensions) {
            if (group.equals(knownExtension.getGroup()) && artifact.equals(knownExtension.getName())
                    && version.equals(knownExtension.getVersion())) {
                return knownExtension;
            }
        }
        return null;
    }
}
