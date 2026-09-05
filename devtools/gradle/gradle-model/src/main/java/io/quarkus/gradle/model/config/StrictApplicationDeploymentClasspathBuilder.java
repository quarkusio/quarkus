package io.quarkus.gradle.model.config;

import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.dependency.QuarkusComponentVariants;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

@SuppressWarnings("UnstableApiUsage")
public final class StrictApplicationDeploymentClasspathBuilder {

    private final Project project;
    private final LaunchMode mode;
    private final String runtimeConfigurationName;
    private final String platformConfigurationName;
    private final String platformPropertiesConfigurationName;
    private final String deploymentConfigurationName;
    private final String compileOnlyConfigurationName;

    public static void initConfigurations(Project project) {
        var configurations = project.getConfigurations();
        configurations.maybeCreate(ToolingUtils.DEV_MODE_CONFIGURATION_NAME)
                .extendsFrom(configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME));
        maybeCreateLegacyBaseConfiguration(project, LaunchMode.TEST, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        maybeCreateLegacyBaseConfiguration(project, LaunchMode.NORMAL, JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        maybeCreateLegacyBaseConfiguration(project, LaunchMode.DEVELOPMENT, ToolingUtils.DEV_MODE_CONFIGURATION_NAME,
                JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME, JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    }

    private static void maybeCreateLegacyBaseConfiguration(Project project, LaunchMode mode, String... parents) {
        String name = ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode);
        if (project.getConfigurations().findByName(name) != null) {
            return;
        }
        project.getConfigurations().resolvable(name, configuration -> {
            configuration.setCanBeConsumed(false);
            for (String parent : parents) {
                configuration.extendsFrom(project.getConfigurations().getByName(parent));
            }
            QuarkusComponentVariants.setConditionalAttributes(configuration, project, mode);
        });
    }

    public StrictApplicationDeploymentClasspathBuilder(Project project, LaunchMode mode, String configurationNamePrefix) {
        this.project = project;
        this.mode = mode;
        runtimeConfigurationName = configurationNamePrefix + launchModeAlias(mode) + "RuntimeClasspathConfiguration";
        platformConfigurationName = ToolingUtils.toPlatformConfigurationName(runtimeConfigurationName);
        platformPropertiesConfigurationName = configurationNamePrefix + launchModeAlias(mode) + "PlatformProperties";
        deploymentConfigurationName = ToolingUtils.toDeploymentConfigurationName(runtimeConfigurationName);
        compileOnlyConfigurationName = configurationNamePrefix + launchModeAlias(mode) + "CompileOnlyConfiguration";

        setUpPlatformConfiguration();
        setUpPlatformPropertiesConfiguration();
        setUpRuntimeConfiguration();
        setUpDeploymentConfiguration();
        setUpCompileOnlyConfiguration();
    }

    private void setUpPlatformConfiguration() {
        if (project.getConfigurations().findByName(platformConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(platformConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            ListProperty<Dependency> dependencies = project.getObjects().listProperty(Dependency.class);
            configuration.getDependencies().addAllLater(dependencies.value(project.provider(() -> project.getConfigurations()
                    .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                    .getAllDependencies()
                    .stream()
                    .filter(dependency -> dependency instanceof ModuleDependency moduleDependency
                            && ToolingUtils.isEnforcedPlatform(moduleDependency))
                    .collect(Collectors.toList()))));
        });
    }

    private void setUpPlatformPropertiesConfiguration() {
        if (project.getConfigurations().findByName(platformPropertiesConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(platformPropertiesConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setTransitive(false);
            DependencyHandler dependencies = project.getDependencies();
            // Platform BOMs constrain their descriptor and properties modules but do not resolve those artifacts.
            configuration.getDependencies().addAllLater(getPlatformConfiguration().getIncoming().getResolutionResult()
                    .getRootComponent()
                    .map(root -> root.getDependencies().stream()
                            .filter(ResolvedDependencyResult.class::isInstance)
                            .map(ResolvedDependencyResult.class::cast)
                            .map(dependency -> dependency.getSelected().getId())
                            .filter(ModuleComponentIdentifier.class::isInstance)
                            .map(ModuleComponentIdentifier.class::cast)
                            .flatMap(platform -> platformArtifactDependencyNotations(platform).stream())
                            .distinct()
                            .sorted()
                            .map(dependencies::create)
                            .toList()));
        });
    }

    private static List<String> platformArtifactDependencyNotations(ModuleComponentIdentifier platform) {
        String groupAndArtifact = platform.getGroup() + ":" + platform.getModule();
        String version = platform.getVersion();
        return List.of(
                groupAndArtifact + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX + ":" + version + ":" + version
                        + "@json",
                groupAndArtifact + BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX + ":" + version + "@properties");
    }

    private void setUpRuntimeConfiguration() {
        if (project.getConfigurations().findByName(runtimeConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(runtimeConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            for (String configurationName : originalRuntimeConfigurationNames(mode)) {
                configuration.extendsFrom(project.getConfigurations().getByName(configurationName));
            }
            QuarkusComponentVariants.setConditionalAttributes(configuration, project, mode);
        });
    }

    private void setUpDeploymentConfiguration() {
        if (project.getConfigurations().findByName(deploymentConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(deploymentConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(getRuntimeConfigurationWithoutResolvingDeployment(), getPlatformConfiguration());
            configuration.shouldResolveConsistentlyWith(getRuntimeConfigurationWithoutResolvingDeployment());
            QuarkusComponentVariants.setDeploymentAndConditionalAttributes(configuration, project, mode);
        });
    }

    private void setUpCompileOnlyConfiguration() {
        if (project.getConfigurations().findByName(compileOnlyConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(compileOnlyConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(getPlatformConfiguration(),
                    project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME));
            configuration.shouldResolveConsistentlyWith(getDeploymentConfiguration());
            QuarkusComponentVariants.setCommonAttributes(configuration.getAttributes(), project.getObjects());
        });
    }

    public FileCollection getOriginalRuntimeClasspathAsInput() {
        return project.files(originalRuntimeConfigurationNames(mode).stream()
                .map(name -> project.getConfigurations().getByName(name))
                .toArray());
    }

    public Configuration getPlatformConfiguration() {
        return project.getConfigurations().getByName(platformConfigurationName);
    }

    public Configuration getPlatformPropertiesConfiguration() {
        return project.getConfigurations().getByName(platformPropertiesConfigurationName);
    }

    public Configuration getRuntimeConfigurationWithoutResolvingDeployment() {
        return project.getConfigurations().getByName(runtimeConfigurationName);
    }

    public Configuration getDeploymentConfiguration() {
        return project.getConfigurations().getByName(deploymentConfigurationName);
    }

    public Configuration getCompileOnlyWithoutResolvingDeployment() {
        return project.getConfigurations().getByName(compileOnlyConfigurationName);
    }

    private static List<String> originalRuntimeConfigurationNames(LaunchMode mode) {
        return switch (mode) {
            case TEST -> List.of(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
            case NORMAL -> List.of(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
            case DEVELOPMENT -> List.of(
                    ToolingUtils.DEV_MODE_CONFIGURATION_NAME,
                    JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME,
                    JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
            default -> throw new IllegalArgumentException("Unsupported launch mode: " + mode);
        };
    }

    private static String launchModeAlias(LaunchMode mode) {
        return switch (mode) {
            case DEVELOPMENT -> "Dev";
            case TEST -> "Test";
            case NORMAL -> "Prod";
            default -> throw new IllegalArgumentException("Unsupported launch mode: " + mode);
        };
    }
}
