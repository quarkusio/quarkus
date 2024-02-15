package io.quarkus.gradle.dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.runtime.LaunchMode;

public class ApplicationDeploymentClasspathBuilder {

    private static String getLaunchModeAlias(LaunchMode mode) {
        if (mode == LaunchMode.DEVELOPMENT) {
            return "Dev";
        }
        if (mode == LaunchMode.TEST) {
            return "Test";
        }
        return "Prod";
    }

    private static String getRuntimeConfigName(LaunchMode mode, boolean base) {
        final StringBuilder sb = new StringBuilder();
        sb.append("quarkus").append(getLaunchModeAlias(mode));
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
        configContainer.register(ToolingUtils.DEV_MODE_CONFIGURATION_NAME, config -> {
            config.extendsFrom(configContainer.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME));
            config.setCanBeConsumed(false);
        });

        // Base runtime configurations for every launch mode
        configContainer
                .register(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.TEST), config -> {
                    config.extendsFrom(configContainer.getByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME));
                    config.setCanBeConsumed(false);
                });

        configContainer
                .register(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.NORMAL), config -> {
                    config.extendsFrom(configContainer.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
                    config.setCanBeConsumed(false);
                });

        configContainer
                .register(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.DEVELOPMENT), config -> {
                    config.extendsFrom(
                            configContainer.getByName(ToolingUtils.DEV_MODE_CONFIGURATION_NAME),
                            configContainer.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME),
                            configContainer.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
                    config.setCanBeConsumed(false);
                });
    }

    private final Project project;
    private final LaunchMode mode;

    private final String runtimeConfigurationName;
    private final String platformConfigurationName;
    private final String deploymentConfigurationName;
    private final String compileOnlyConfigurationName;

    /**
     * The platform configuration updates the PlatformImports, but since the PlatformImports don't
     * have a place to be stored in the project, they're stored here. The way that extensions are
     * tracked and conditional dependencies needs some attention, which will likely resolve this.
     */
    private static final HashMap<String, PlatformImportsImpl> platformImports = new HashMap<>();
    /**
     * The key used to look up the correct PlatformImports that matches the platformConfigurationName
     */
    private final String platformImportName;

    public ApplicationDeploymentClasspathBuilder(Project project, LaunchMode mode) {
        this.project = project;
        this.mode = mode;
        this.runtimeConfigurationName = getFinalRuntimeConfigName(mode);
        this.platformConfigurationName = ToolingUtils.toPlatformConfigurationName(this.runtimeConfigurationName);
        this.deploymentConfigurationName = ToolingUtils.toDeploymentConfigurationName(this.runtimeConfigurationName);
        this.platformImportName = project.getPath() + ":" + this.platformConfigurationName;
        this.compileOnlyConfigurationName = "quarkus" + getLaunchModeAlias(mode) + "CompileOnlyConfiguration";

        setUpPlatformConfiguration();
        setUpRuntimeConfiguration();
        setUpDeploymentConfiguration();
        setUpCompileOnlyConfiguration();
    }

    private void setUpPlatformConfiguration() {
        if (project.getConfigurations().findByName(this.platformConfigurationName) == null) {
            PlatformImportsImpl platformImports = ApplicationDeploymentClasspathBuilder.platformImports
                    .computeIfAbsent(this.platformImportName, (ignored) -> new PlatformImportsImpl());

            project.getConfigurations().register(this.platformConfigurationName, configuration -> {
                configuration.setCanBeConsumed(false);
                // Platform configuration is just implementation, filtered to platform dependencies
                ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
                configuration.getDependencies()
                        .addAllLater(dependencyListProperty.value(project.provider(() -> project.getConfigurations()
                                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                                .getAllDependencies()
                                .stream()
                                .filter(dependency -> dependency instanceof ModuleDependency &&
                                        ToolingUtils.isEnforcedPlatform((ModuleDependency) dependency))
                                .collect(Collectors.toList()))));
                // Configures PlatformImportsImpl once the platform configuration is resolved
                configuration.getResolutionStrategy().eachDependency(d -> {
                    ModuleIdentifier identifier = d.getTarget().getModule();
                    final String group = identifier.getGroup();
                    final String name = identifier.getName();
                    if (name.endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                        platformImports.addPlatformDescriptor(group, name, d.getTarget().getVersion(), "json",
                                d.getTarget().getVersion());
                    } else if (name.endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                        final DefaultDependencyArtifact dep = new DefaultDependencyArtifact();
                        dep.setExtension("properties");
                        dep.setType("properties");
                        dep.setName(name);

                        final DefaultExternalModuleDependency gradleDep = new DefaultExternalModuleDependency(
                                group, name, d.getTarget().getVersion(), null);
                        gradleDep.addArtifact(dep);

                        for (ResolvedArtifact a : project.getConfigurations().detachedConfiguration(gradleDep)
                                .getResolvedConfiguration().getResolvedArtifacts()) {
                            if (a.getName().equals(name)) {
                                try {
                                    platformImports.addPlatformProperties(group, name, null, "properties",
                                            d.getTarget().getVersion(),
                                            a.getFile().toPath());
                                } catch (AppModelResolverException e) {
                                    throw new GradleException("Failed to import platform properties " + a.getFile(), e);
                                }
                                break;
                            }
                        }
                    }
                });
            });
        }
    }

    private void setUpRuntimeConfiguration() {
        if (!project.getConfigurations().getNames().contains(this.runtimeConfigurationName)) {
            project.getConfigurations().register(this.runtimeConfigurationName, configuration -> {
                configuration.setCanBeConsumed(false);
                configuration.extendsFrom(
                        project.getConfigurations()
                                .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode)));
            });
        }
    }

    private void setUpDeploymentConfiguration() {
        if (!project.getConfigurations().getNames().contains(this.deploymentConfigurationName)) {
            project.getConfigurations().register(this.deploymentConfigurationName, configuration -> {
                configuration.setCanBeConsumed(false);
                Configuration enforcedPlatforms = this.getPlatformConfiguration();
                configuration.extendsFrom(enforcedPlatforms);
                ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
                configuration.getDependencies().addAllLater(dependencyListProperty.value(project.provider(() -> {
                    ConditionalDependenciesEnabler cdEnabler = new ConditionalDependenciesEnabler(project, mode,
                            enforcedPlatforms);
                    final Collection<ExtensionDependency<?>> allExtensions = cdEnabler.getAllExtensions();
                    Set<ExtensionDependency<?>> extensions = collectFirstMetQuarkusExtensions(getRawRuntimeConfiguration(),
                            allExtensions);
                    // Add conditional extensions
                    for (ExtensionDependency<?> knownExtension : allExtensions) {
                        if (knownExtension.isConditional()) {
                            extensions.add(knownExtension);
                        }
                    }

                    final Set<ModuleVersionIdentifier> alreadyProcessed = new HashSet<>(extensions.size());
                    final DependencyHandler dependencies = project.getDependencies();
                    final Set<Dependency> deploymentDependencies = new HashSet<>();
                    for (ExtensionDependency<?> extension : extensions) {
                        if (!alreadyProcessed.add(extension.getExtensionId())) {
                            continue;
                        }

                        deploymentDependencies.add(
                                DependencyUtils.createDeploymentDependency(dependencies, extension));
                    }
                    return deploymentDependencies;
                })));
            });
        }
    }

    private void setUpCompileOnlyConfiguration() {
        if (!project.getConfigurations().getNames().contains(compileOnlyConfigurationName)) {
            project.getConfigurations().register(compileOnlyConfigurationName, config -> {
                config.extendsFrom(project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME));
                config.shouldResolveConsistentlyWith(getDeploymentConfiguration());
                config.setCanBeConsumed(false);
            });
        }
    }

    public Configuration getPlatformConfiguration() {
        return project.getConfigurations().getByName(this.platformConfigurationName);
    }

    private Configuration getRawRuntimeConfiguration() {
        return project.getConfigurations().getByName(this.runtimeConfigurationName);
    }

    /**
     * Forces deployment configuration to resolve to discover conditional dependencies.
     */
    public Configuration getRuntimeConfiguration() {
        this.getDeploymentConfiguration().resolve();
        return project.getConfigurations().getByName(this.runtimeConfigurationName);
    }

    public Configuration getDeploymentConfiguration() {
        return project.getConfigurations().getByName(this.deploymentConfigurationName);
    }

    /**
     * Compile-only configuration which is consistent with the deployment one
     */
    public Configuration getCompileOnly() {
        this.getDeploymentConfiguration().resolve();
        return project.getConfigurations().getByName(compileOnlyConfigurationName);
    }

    /**
     * Forces the platform configuration to resolve and then uses that to populate platform imports.
     */
    public PlatformImports getPlatformImports() {
        this.getPlatformConfiguration().getResolvedConfiguration();
        return platformImports.get(this.platformImportName);
    }

    private Set<ExtensionDependency<?>> collectFirstMetQuarkusExtensions(Configuration configuration,
            Collection<ExtensionDependency<?>> knownExtensions) {

        Set<ExtensionDependency<?>> firstLevelExtensions = new HashSet<>();
        Set<ResolvedDependency> firstLevelModuleDependencies = configuration.getResolvedConfiguration()
                .getFirstLevelModuleDependencies();

        Set<String> visitedArtifacts = new HashSet<>();
        for (ResolvedDependency firstLevelModuleDependency : firstLevelModuleDependencies) {
            firstLevelExtensions
                    .addAll(collectQuarkusExtensions(firstLevelModuleDependency, visitedArtifacts, knownExtensions));
        }
        return firstLevelExtensions;
    }

    private Set<ExtensionDependency<?>> collectQuarkusExtensions(ResolvedDependency dependency, Set<String> visitedArtifacts,
            Collection<ExtensionDependency<?>> knownExtensions) {
        String artifactKey = String.format("%s:%s", dependency.getModuleGroup(), dependency.getModuleName());
        if (!visitedArtifacts.add(artifactKey)) {
            return Collections.emptySet();
        }

        Set<ExtensionDependency<?>> extensions = new LinkedHashSet<>();
        ExtensionDependency<?> extension = getExtensionOrNull(dependency.getModuleGroup(), dependency.getModuleName(),
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

    private ExtensionDependency<?> getExtensionOrNull(String group, String artifact, String version,
            Collection<ExtensionDependency<?>> knownExtensions) {
        for (ExtensionDependency<?> knownExtension : knownExtensions) {
            if (group.equals(knownExtension.getGroup()) && artifact.equals(knownExtension.getName())
                    && version.equals(knownExtension.getVersion())) {
                return knownExtension;
            }
        }
        return null;
    }
}
