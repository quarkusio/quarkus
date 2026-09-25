package io.quarkus.gradle.application.internal.modelgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;

import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.runtime.LaunchMode;

public final class ClasspathBuilder {

    private static final String RUNTIME_CONFIGURATION_NAME = "quarkusApplicationRuntimeClasspathConfiguration";
    private static final String DEV_BASE_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationDevBaseRuntimeClasspathConfiguration";
    private static final String DEV_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationDevRuntimeClasspathConfiguration";
    private static final String TEST_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationTestRuntimeClasspathConfiguration";
    private static final String CONTINUOUS_TEST_BASE_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationContinuousTestBaseRuntimeClasspathConfiguration";
    private static final String CONTINUOUS_TEST_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationContinuousTestRuntimeClasspathConfiguration";
    private static final String CONDITIONAL_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationConditionalRuntimeClasspathConfiguration";
    private static final String DEV_CONDITIONAL_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationDevConditionalRuntimeClasspathConfiguration";
    private static final String TEST_CONDITIONAL_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationTestConditionalRuntimeClasspathConfiguration";
    private static final String CONTINUOUS_TEST_CONDITIONAL_RUNTIME_CONFIGURATION_NAME = "quarkusApplicationContinuousTestConditionalRuntimeClasspathConfiguration";
    private static final String LOCAL_CONDITIONAL_DEV_DEPENDENCIES_CONFIGURATION_NAME = "quarkusApplicationLocalConditionalDevDependenciesConfiguration";
    private static final String CONTINUOUS_TEST_LOCAL_CONDITIONAL_DEV_DEPENDENCIES_CONFIGURATION_NAME = "quarkusApplicationContinuousTestLocalConditionalDevDependenciesConfiguration";
    private static final String DEPLOYMENT_CONFIGURATION_NAME = "quarkusApplicationDeploymentClasspathConfiguration";
    private static final String DEV_DEPLOYMENT_CONFIGURATION_NAME = "quarkusApplicationDevDeploymentClasspathConfiguration";
    private static final String TEST_DEPLOYMENT_CONFIGURATION_NAME = "quarkusApplicationTestDeploymentClasspathConfiguration";
    private static final String CONTINUOUS_TEST_DEPLOYMENT_CONFIGURATION_NAME = "quarkusApplicationContinuousTestDeploymentClasspathConfiguration";
    private static final String COMPILE_ONLY_CONFIGURATION_NAME = "quarkusApplicationCompileOnlyConfiguration";
    private static final String TEST_COMPILE_ONLY_CONFIGURATION_NAME = "quarkusApplicationTestCompileOnlyConfiguration";
    private static final String PLATFORM_DECLARATIONS_CONFIGURATION_NAME = "quarkusApplicationPlatformDeclarations";
    private static final String PLATFORM_CONFIGURATION_NAME = "quarkusApplicationPlatformConfiguration";
    private static final String PLATFORM_PROPERTIES_CONFIGURATION_NAME = "quarkusApplicationPlatformProperties";

    private final Project project;
    private final LocalExtensionDependencyRegistration localExtensionDependencies;

    public ClasspathBuilder(Project project, Configuration developmentDependencies) {
        this.project = project;
        this.localExtensionDependencies = new LocalExtensionDependencyRegistration(project);
        Configuration devBaseRuntime = setUpDevBaseRuntimeConfiguration(developmentDependencies);
        setUpRuntimeConfiguration(RUNTIME_CONFIGURATION_NAME, CONDITIONAL_RUNTIME_CONFIGURATION_NAME, LaunchMode.NORMAL,
                getRawRuntimeConfiguration());
        setUpRuntimeConfiguration(DEV_RUNTIME_CONFIGURATION_NAME, DEV_CONDITIONAL_RUNTIME_CONFIGURATION_NAME,
                LaunchMode.DEVELOPMENT, devBaseRuntime);
        setUpRuntimeConfiguration(TEST_RUNTIME_CONFIGURATION_NAME, TEST_CONDITIONAL_RUNTIME_CONFIGURATION_NAME, LaunchMode.TEST,
                getRawTestRuntimeConfiguration());
        Configuration continuousTestBaseRuntime = setUpContinuousTestBaseRuntimeConfiguration(developmentDependencies);
        setUpRuntimeConfiguration(CONTINUOUS_TEST_RUNTIME_CONFIGURATION_NAME,
                CONTINUOUS_TEST_CONDITIONAL_RUNTIME_CONFIGURATION_NAME, LaunchMode.TEST, true, continuousTestBaseRuntime);
        setUpDeploymentConfiguration(DEPLOYMENT_CONFIGURATION_NAME, getRuntimeConfiguration());
        setUpDeploymentConfiguration(DEV_DEPLOYMENT_CONFIGURATION_NAME, getDevRuntimeConfiguration());
        setUpDeploymentConfiguration(TEST_DEPLOYMENT_CONFIGURATION_NAME, getTestRuntimeConfiguration());
        setUpDeploymentConfiguration(CONTINUOUS_TEST_DEPLOYMENT_CONFIGURATION_NAME,
                getContinuousTestRuntimeConfiguration());
        setUpCompileOnlyConfiguration(COMPILE_ONLY_CONFIGURATION_NAME,
                JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME);
        setUpCompileOnlyConfiguration(TEST_COMPILE_ONLY_CONFIGURATION_NAME,
                JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME);
        new PlatformClasspathRegistration().register(project, PLATFORM_DECLARATIONS_CONFIGURATION_NAME,
                PLATFORM_CONFIGURATION_NAME, PLATFORM_PROPERTIES_CONFIGURATION_NAME);
    }

    private Configuration setUpDevBaseRuntimeConfiguration(Configuration developmentDependencies) {
        return project.getConfigurations().resolvable(DEV_BASE_RUNTIME_CONFIGURATION_NAME, configuration -> {
            configuration.setDescription("Internal base runtime classpath for Quarkus development.");
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());
            configuration.extendsFrom(developmentDependencies, getRawCompileClasspathConfiguration(),
                    getRawRuntimeConfiguration());
        }).get();
    }

    private Configuration setUpContinuousTestBaseRuntimeConfiguration(Configuration developmentDependencies) {
        return project.getConfigurations().resolvable(CONTINUOUS_TEST_BASE_RUNTIME_CONFIGURATION_NAME, configuration -> {
            configuration.setDescription("Internal base runtime classpath for Quarkus continuous testing.");
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());
            configuration.extendsFrom(developmentDependencies, getRawTestRuntimeConfiguration());
        }).get();
    }

    public FileCollection getOriginalRuntimeClasspathAsInput() {
        return project.files(getRawRuntimeConfiguration(), getRuntimeConfiguration());
    }

    public FileCollection getOriginalTestRuntimeClasspathAsInput() {
        return project.files(getRawTestRuntimeConfiguration(), getTestRuntimeConfiguration());
    }

    public FileCollection getOriginalContinuousTestRuntimeClasspathAsInput() {
        return project.files(getRawTestRuntimeConfiguration(), getContinuousTestRuntimeConfiguration());
    }

    public FileCollection getOriginalDevRuntimeClasspathAsInput() {
        return project.files(getRawCompileClasspathConfiguration(), getRawRuntimeConfiguration(), getDevRuntimeConfiguration());
    }

    public Configuration getRuntimeConfiguration() {
        return project.getConfigurations().getByName(RUNTIME_CONFIGURATION_NAME);
    }

    public Configuration getDevRuntimeConfiguration() {
        return project.getConfigurations().getByName(DEV_RUNTIME_CONFIGURATION_NAME);
    }

    public Configuration getTestRuntimeConfiguration() {
        return project.getConfigurations().getByName(TEST_RUNTIME_CONFIGURATION_NAME);
    }

    public Configuration getContinuousTestRuntimeConfiguration() {
        return project.getConfigurations().getByName(CONTINUOUS_TEST_RUNTIME_CONFIGURATION_NAME);
    }

    public Configuration getDeploymentConfiguration() {
        return project.getConfigurations().getByName(DEPLOYMENT_CONFIGURATION_NAME);
    }

    public Configuration getDevDeploymentConfiguration() {
        return project.getConfigurations().getByName(DEV_DEPLOYMENT_CONFIGURATION_NAME);
    }

    public Configuration getTestDeploymentConfiguration() {
        return project.getConfigurations().getByName(TEST_DEPLOYMENT_CONFIGURATION_NAME);
    }

    public Configuration getContinuousTestDeploymentConfiguration() {
        return project.getConfigurations().getByName(CONTINUOUS_TEST_DEPLOYMENT_CONFIGURATION_NAME);
    }

    public Configuration getCompileOnlyConfiguration() {
        return project.getConfigurations().getByName(COMPILE_ONLY_CONFIGURATION_NAME);
    }

    public Configuration getTestCompileOnlyConfiguration() {
        return project.getConfigurations().getByName(TEST_COMPILE_ONLY_CONFIGURATION_NAME);
    }

    public Configuration getPlatformPropertiesConfiguration() {
        return project.getConfigurations().getByName(PLATFORM_PROPERTIES_CONFIGURATION_NAME);
    }

    public List<Configuration> getOfflinePreparationConfigurations() {
        return List.of(
                getRuntimeConfiguration(),
                getDevRuntimeConfiguration(),
                getTestRuntimeConfiguration(),
                getContinuousTestRuntimeConfiguration(),
                getDeploymentConfiguration(),
                getDevDeploymentConfiguration(),
                getTestDeploymentConfiguration(),
                getContinuousTestDeploymentConfiguration(),
                getCompileOnlyConfiguration(),
                getTestCompileOnlyConfiguration(),
                project.getConfigurations().getByName(PLATFORM_CONFIGURATION_NAME),
                getPlatformPropertiesConfiguration());
    }

    private Configuration getRawRuntimeConfiguration() {
        return project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    }

    private Configuration getRawTestRuntimeConfiguration() {
        return project.getConfigurations().getByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    }

    private Configuration getRawCompileClasspathConfiguration() {
        return project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
    }

    private void setUpRuntimeConfiguration(
            String runtimeConfigurationName,
            String conditionalRuntimeConfigurationName,
            LaunchMode launchMode,
            Configuration... rawRuntimeConfigurations) {
        setUpRuntimeConfiguration(runtimeConfigurationName, conditionalRuntimeConfigurationName, launchMode,
                launchMode == LaunchMode.DEVELOPMENT, rawRuntimeConfigurations);
    }

    private void setUpRuntimeConfiguration(
            String runtimeConfigurationName,
            String conditionalRuntimeConfigurationName,
            LaunchMode launchMode,
            boolean includeConditionalDevDependencies,
            Configuration... rawRuntimeConfigurations) {
        setUpConditionalRuntimeConfiguration(conditionalRuntimeConfigurationName, rawRuntimeConfigurations);
        Provider<List<String>> localConditionalDevCoordinates = includeConditionalDevDependencies
                ? localExtensionDependencies.conditionalDevDependencyCoordinates(
                        localExtensionDependencies.registerConditionalDevDependenciesConfiguration(
                                launchMode == LaunchMode.DEVELOPMENT
                                        ? LOCAL_CONDITIONAL_DEV_DEPENDENCIES_CONFIGURATION_NAME
                                        : CONTINUOUS_TEST_LOCAL_CONDITIONAL_DEV_DEPENDENCIES_CONFIGURATION_NAME,
                                rawRuntimeConfigurations))
                : project.provider(List::of);
        if (project.getConfigurations().findByName(runtimeConfigurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(runtimeConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());
            configuration.extendsFrom(rawRuntimeConfigurations);

            DependencyHandler dependencyHandler = project.getDependencies();
            // Reduce resolved graphs to serial coordinate values inside ValueSources,
            // then let addAllLater materialize dependencies only when Gradle resolves
            // this configuration. No resolved Gradle model object escapes that boundary.
            var satisfiedConditionalDependencies = project.getProviders().of(
                    SatisfiedConditionalDependencyCoordinatesValueSource.class,
                    spec -> {
                        spec.getParameters().getRuntimeComponentKeys()
                                .set(componentKeys(rawRuntimeConfigurations));
                        spec.getParameters().getConditionalArtifactRecords()
                                .set(artifactRecords(project.getConfigurations()
                                        .getByName(conditionalRuntimeConfigurationName)));
                    });
            configuration.getDependencies().addAllLater(satisfiedConditionalDependencies
                    .map(coordinates -> coordinates.stream()
                            .map(dependencyHandler::create)
                            .toList()));
            if (includeConditionalDevDependencies) {
                var conditionalDevDependencies = project.getProviders().of(ConditionalDevDependencyCoordinatesValueSource.class,
                        spec -> configureExternalRuntimeArtifacts(spec.getParameters().getRuntimeArtifacts(),
                                rawRuntimeConfigurations));
                configuration.getDependencies().addAllLater(conditionalDevDependencies
                        .map(coordinates -> coordinates.stream()
                                .map(dependencyHandler::create)
                                .toList()));
                configuration.getDependencies().addAllLater(localConditionalDevCoordinates.map(
                        coordinates -> coordinates.stream()
                                .map(dependencyHandler::create)
                                .toList()));
            }
        });
    }

    private void setUpConditionalRuntimeConfiguration(String configurationName, Configuration... rawRuntimeConfigurations) {
        if (project.getConfigurations().findByName(configurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(configurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());

            DependencyHandler dependencyHandler = project.getDependencies();
            // Conditional metadata is discovered from resolved artifacts, so retain
            // it behind a ValueSource instead of resolving during plugin application.
            var conditionalDependencies = project.getProviders().of(ConditionalDependencyCoordinatesValueSource.class,
                    spec -> configureExternalRuntimeArtifacts(spec.getParameters().getRuntimeArtifacts(),
                            rawRuntimeConfigurations));
            configuration.getDependencies().addAllLater(conditionalDependencies
                    .map(coordinates -> coordinates.stream()
                            .map(dependencyHandler::create)
                            .toList()));
            configuration.getDependencies().addAllLater(localExtensionDependencies.variantDependencies(
                    ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEPENDENCIES_CATEGORY,
                    ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEPENDENCIES_ATTRIBUTE,
                    rawRuntimeConfigurations));
        });
    }

    private void configureExternalRuntimeArtifacts(org.gradle.api.file.ConfigurableFileCollection runtimeArtifacts,
            Configuration... rawRuntimeConfigurations) {
        for (Configuration rawRuntimeConfiguration : rawRuntimeConfigurations) {
            runtimeArtifacts.from(externalRuntimeArtifactFiles(rawRuntimeConfiguration));
        }
    }

    private void setUpDeploymentConfiguration(String configurationName, Configuration runtimeConfiguration) {
        if (project.getConfigurations().findByName(configurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(configurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());
            DependencyHandler dependencyHandler = project.getDependencies();
            ObjectFactory objects = project.getObjects();
            // External and local deployment dependencies are reduced to serial specs
            // and deterministically merged before Gradle materializes this classpath.
            var deploymentArtifacts = project.getProviders().of(DeploymentArtifactsValueSource.class,
                    spec -> spec.getParameters().getRuntimeArtifacts()
                            .from(externalRuntimeArtifactFiles(runtimeConfiguration)));
            configuration.getDependencies().addAllLater(deploymentArtifacts
                    .zip(localExtensionDependencies.deploymentDependencySpecs(runtimeConfiguration),
                            (externalSpecs, localSpecs) -> java.util.stream.Stream.concat(
                                    externalSpecs.stream(),
                                    java.util.stream.StreamSupport.stream(localSpecs.spliterator(), false))
                                    .distinct()
                                    .sorted()
                                    .toList())
                    .map(specs -> specs
                            .stream()
                            .map(spec -> localExtensionDependencies.deploymentDependency(
                                    dependencyHandler, objects, spec))
                            .toList()));
        });
    }

    private Provider<List<String>> componentKeys(Configuration... configurations) {
        Provider<List<String>> componentKeys = project.getProviders().provider(List::of);
        for (Configuration configuration : configurations) {
            Provider<List<String>> configurationComponentKeys = configuration.getIncoming().getResolutionResult()
                    .getRootComponent().map(root -> {
                        Set<ComponentIdentifier> visited = new HashSet<>();
                        Set<String> keys = new TreeSet<>();
                        collectComponentKeys(root, visited, keys);
                        return keys.stream().toList();
                    });
            componentKeys = componentKeys.zip(configurationComponentKeys, (existing, additional) -> {
                Set<String> merged = new TreeSet<>(existing);
                merged.addAll(additional);
                return merged.stream().toList();
            });
        }
        return componentKeys;
    }

    private static void collectComponentKeys(
            ResolvedComponentResult component,
            java.util.Set<ComponentIdentifier> visited,
            java.util.Set<String> keys) {
        if (!visited.add(component.getId())) {
            return;
        }
        if (component.getId() instanceof ModuleComponentIdentifier module) {
            keys.add(ConditionalDependencyResolver.serializeKey(ArtifactCoords.of(
                    module.getGroup(), module.getModule(), ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR,
                    module.getVersion()).getKey()));
        }
        for (DependencyResult dependency : component.getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult resolved) {
                collectComponentKeys(resolved.getSelected(), visited, keys);
            }
        }
    }

    private Provider<Iterable<String>> artifactRecords(Configuration configuration) {
        return configuration.getIncoming().getArtifacts().getResolvedArtifacts()
                .zip(configuration.getIncoming().getResolutionResult().getRootComponent(),
                        (artifacts, root) -> {
                            Set<ComponentIdentifier> candidateComponentIds = conditionalCandidateComponentIds(root);
                            return artifacts.stream()
                                    .filter(artifact -> candidateComponentIds
                                            .contains(artifact.getId().getComponentIdentifier()))
                                    .flatMap(artifact -> artifactRecord(artifact).stream())
                                    .sorted()
                                    .toList();
                        });
    }

    private static Set<ComponentIdentifier> conditionalCandidateComponentIds(ResolvedComponentResult root) {
        Set<ComponentIdentifier> componentIds = new HashSet<>();
        for (DependencyResult dependency : root.getDependencies()) {
            if (!(dependency instanceof ResolvedDependencyResult resolved)) {
                continue;
            }
            ResolvedComponentResult selected = resolved.getSelected();
            if (LocalExtensionDependencyRegistration.selectedVariant(selected,
                    ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEPENDENCIES_ATTRIBUTE)) {
                for (DependencyResult conditionalDependency : selected.getDependencies()) {
                    if (conditionalDependency instanceof ResolvedDependencyResult conditionalResolved) {
                        componentIds.add(conditionalResolved.getSelected().getId());
                    }
                }
            } else {
                componentIds.add(selected.getId());
            }
        }
        return componentIds;
    }

    private static java.util.Optional<String> artifactRecord(ResolvedArtifactResult artifact) {
        if (!(artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier componentIdentifier)) {
            return java.util.Optional.empty();
        }
        String type = artifact.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
        if (type == null || type.isBlank()) {
            type = ArtifactCoords.TYPE_JAR;
        }
        return java.util.Optional.of(new ArtifactRecord(
                componentIdentifier.getGroup(),
                componentIdentifier.getModule(),
                componentIdentifier.getVersion(),
                ArtifactCoords.DEFAULT_CLASSIFIER,
                type,
                artifact.getFile()).serialize());
    }

    private FileCollection externalRuntimeArtifactFiles(Configuration configuration) {
        return configuration.getIncoming()
                .artifactView(view -> view.componentFilter(ModuleComponentIdentifier.class::isInstance))
                .getFiles();
    }

    private void setUpCompileOnlyConfiguration(String configurationName, String... extendsFrom) {
        if (project.getConfigurations().findByName(configurationName) != null) {
            return;
        }
        project.getConfigurations().resolvable(configurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            setJavaRuntimeAttributes(configuration.getAttributes());
            for (String parent : extendsFrom) {
                configuration.extendsFrom(project.getConfigurations().getByName(parent));
            }
        });
    }

    private void setJavaRuntimeAttributes(AttributeContainer attributes) {
        ObjectFactory objects = project.getObjects();
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
        attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
    }

}
