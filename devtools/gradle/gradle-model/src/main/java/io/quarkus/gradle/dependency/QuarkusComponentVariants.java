package io.quarkus.gradle.dependency;

import static io.quarkus.gradle.tooling.dependency.DependencyUtils.getKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.runtime.LaunchMode;

/**
 * Analyzes application configurations and adds the necessary component variants
 * to enable conditional and deployment dependencies.
 */
public class QuarkusComponentVariants {

    private static final String ON = "on";
    private static final String RUNTIME_ELEMENTS = "runtimeElements";
    private static final String RUNTIME = "runtime";

    /**
     * Makes sure the first character of the string is an upper-case one.
     *
     * @param s a string
     * @return a string that starts with an upper-case character
     */
    private static String toUpperCaseName(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Returns a runtime classpath configuration name for a specific launch mode
     * that includes satisfied conditional dependencies.
     *
     * @param mode launch mode
     * @return conditional runtime classpath configuration name
     */
    public static String getConditionalConfigurationName(LaunchMode mode) {
        return "quarkusConditional" + ApplicationDeploymentClasspathBuilder.getLaunchModeAlias(mode)
                + "RuntimeClasspath";
    }

    /**
     * Returns a conditional dependency attribute for a specific project and launch mode.
     * A project name is included in the attribute name since the same dependency may end up being used
     * in different subprojects of the same codebase.
     *
     * @param projectName project name
     * @param mode launch mode
     * @return conditional dependency attribute
     */
    private static Attribute<String> getConditionalDependencyAttribute(String projectName, LaunchMode mode) {
        var sb = new StringBuilder()
                .append("quarkus.")
                .append(mode.getDefaultProfile())
                .append(".conditional-dependency.")
                .append(projectName);
        return Attribute.of(sb.toString(), String.class);
    }

    /**
     * Returns a deployment dependency attribute for a specific project and launch mode.
     * A project name is included in the attribute name since the same dependency may end up being used
     * in different subprojects of the same codebase.
     *
     * @param projectName project name
     * @param mode launch mode
     * @return deployment dependency attribute
     */
    private static Attribute<String> getDeploymentDependencyAttribute(String projectName, LaunchMode mode) {
        var sb = new StringBuilder()
                .append("quarkus.")
                .append(mode.getDefaultProfile())
                .append(".deployment-dependency.")
                .append(projectName);
        return Attribute.of(sb.toString(), String.class);
    }

    /**
     * Sets conditional and other generally relevant attributes on a configuration for a given project and launch mode.
     *
     * @param config target configuration
     * @param project project
     * @param mode launch mode
     */
    public static void setConditionalAttributes(Configuration config, Project project, LaunchMode mode) {
        config.attributes(attrs -> {
            setCommonAttributes(attrs, project.getObjects());
            attrs.attribute(getConditionalDependencyAttribute(project.getName(), mode), ON);
        });
    }

    /**
     * Sets deployment, conditional and other generally relevant attributes on a configuration for a given project
     * and launch mode.
     *
     * @param config target configuration
     * @param project project
     * @param mode launch mode
     */
    public static void setDeploymentAndConditionalAttributes(Configuration config, Project project, LaunchMode mode) {
        setConditionalAttributes(config, project, mode);
        config.attributes(attrs -> attrs.attribute(getDeploymentDependencyAttribute(project.getName(), mode), ON));
    }

    /**
     * Sets iterative (not yet final) conditional attributes on a configuration for a given project.
     *
     * @param config target configuration
     * @param project project
     * @param satisfiedExtDeps currently satisfied conditional dependencies
     */
    private static void setIterativeConditionalAttributes(Configuration config, Project project,
            Collection<SatisfiedExtensionDeps> satisfiedExtDeps) {
        config.attributes(attrs -> {
            setCommonAttributes(attrs, project.getObjects());
            for (var satisfiedDep : satisfiedExtDeps) {
                satisfiedDep.setItrativeAttribute(attrs);
            }
        });
    }

    /**
     * Sets generally relevant non-Quarkus attributes.
     *
     * @param attrs attributes container
     * @param objectFactory object factory
     */
    public static void setCommonAttributes(AttributeContainer attrs, ObjectFactory objectFactory) {
        attrs.attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.LIBRARY));
        attrs.attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.class, Usage.JAVA_RUNTIME));
        attrs.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objectFactory.named(LibraryElements.class, LibraryElements.JAR));
        attrs.attribute(Bundling.BUNDLING_ATTRIBUTE, objectFactory.named(Bundling.class, Bundling.EXTERNAL));
        attrs.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objectFactory.named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
    }

    /**
     * Analyzes project configurations and adds the necessary component attributes for a specific launch mode.
     *
     * @param project project
     * @param mode launch mode
     */
    public static void addVariants(Project project, LaunchMode mode,
            Property<PlatformSpec> platformSpecProperty) {
        new QuarkusComponentVariants(project, mode, platformSpecProperty).configureAndAddVariants();
    }

    private final Attribute<String> quarkusDepAttr;
    private final Project project;
    private final Property<PlatformSpec> platformSpecProperty;
    private final Map<ArtifactKey, ProcessedDependency> processedDeps = new HashMap<>();
    private final Map<ArtifactKey, ConditionalDependency> allConditionalDeps = new HashMap<>();
    private final List<ConditionalDependencyVariant> dependencyVariantQueue = new ArrayList<>();
    private final Map<String, SatisfiedExtensionDeps> satisfiedExtensionDeps = new HashMap<>();
    private final LaunchMode mode;
    private final AtomicInteger configCopyCounter = new AtomicInteger();

    private QuarkusComponentVariants(Project project, LaunchMode mode,
            Property<PlatformSpec> platformSpecProperty) {
        this.project = project;
        this.mode = mode;
        this.platformSpecProperty = platformSpecProperty;
        this.quarkusDepAttr = getConditionalDependencyAttribute(project.getName(), mode);
        project.getDependencies().getAttributesSchema().attribute(quarkusDepAttr);
        project.getDependencies().getAttributesSchema().attribute(getDeploymentDependencyAttribute(project.getName(), mode));
    }

    /**
     * Configuration that should be used as the base for conditional and deployment dependency analysis.
     *
     * @return base configuration for dependency analysis
     */
    private Configuration getBaseConfiguration() {
        return project.getConfigurations().getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode));
    }

    /**
     * Registers a runtime configuration with enabled conditional dependencies.
     * It also adds deployment variants to the corresponding components but does not select them in this configuration.
     */
    private void configureAndAddVariants() {
        project.getConfigurations().resolvable(
                getConditionalConfigurationName(mode),
                config -> {
                    config.setCanBeConsumed(false);
                    config.extendsFrom(getBaseConfiguration());
                    setConditionalAttributes(config, project, mode);
                    final ListProperty<Dependency> dependencyProperty = project.getObjects().listProperty(Dependency.class);
                    final AtomicInteger invocations = new AtomicInteger();
                    config.getDependencies().addAllLater(dependencyProperty.value(project.provider(() -> {
                        if (invocations.getAndIncrement() == 0) {
                            addConditionalVariants(getBaseConfiguration());
                            addDeploymentVariants();
                        }
                        return Set.of();
                    })));
                });
    }

    /**
     * This is where the dependencies are being analyzed and component variants bein added.
     *
     * @param baseConfig base configuration for analysis
     */
    private void addConditionalVariants(Configuration baseConfig) {
        processConfiguration(baseConfig);
        while (!dependencyVariantQueue.isEmpty()) {
            boolean satisfiedConditions = false;
            var i = dependencyVariantQueue.iterator();
            while (i.hasNext()) {
                var conditionalVariant = i.next();
                if (conditionalVariant.conditionalDep.isConditionSatisfied()) {
                    satisfiedConditions = true;
                    i.remove();
                    satisfiedExtensionDeps.computeIfAbsent(conditionalVariant.parent.toModuleName(),
                            key -> new SatisfiedExtensionDeps(conditionalVariant.parent, project.getName(), mode)).deps
                            .add(conditionalVariant.conditionalDep);
                }
            }
            if (!satisfiedConditions) {
                break;
            }
            for (var satisfiedDeps : satisfiedExtensionDeps.values()) {
                project.getDependencies().getComponents().withModule(
                        satisfiedDeps.getModuleId(),
                        satisfiedDeps::addIterativeConditionalVariants);
            }
            processConfiguration(baseConfig);
        }
    }

    private void addConditionalVariant(String variantName, ComponentMetadataDetails compDetails,
            Attribute<String> attr, List<ConditionalDependency> deps) {
        addConditionalVariant(variantName, RUNTIME_ELEMENTS, compDetails, attr, deps);
        addConditionalVariant(variantName, RUNTIME, compDetails, attr, deps);
    }

    private void addConditionalVariant(String variantName, String baseVariant,
            ComponentMetadataDetails compDetails,
            Attribute<String> attr,
            List<ConditionalDependency> satisfiedDeps) {
        compDetails.maybeAddVariant(
                variantName,
                baseVariant,
                variant -> {
                    final AtomicInteger selectCounter = new AtomicInteger();
                    variant.attributes(attrs -> {
                        if (selectCounter.getAndIncrement() == 0) {
                            attrs.attribute(attr, ON);
                        }
                    });
                    variant.withDependencies(directDeps -> {
                        for (var satisfiedDep : satisfiedDeps) {
                            boolean alreadyAdded = false;
                            for (var directDep : directDeps) {
                                if (directDep.getName().equals(satisfiedDep.key.getArtifactId())
                                        && directDep.getGroup().equals(satisfiedDep.key.getGroupId())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                var a = satisfiedDep.artifact;
                                directDeps.add(DependencyUtils.asDependencyNotation(project.getDependencyFactory().create(
                                        a.getModuleVersion().getId().getGroup(),
                                        a.getName(),
                                        a.getModuleVersion().getId().getVersion(),
                                        a.getClassifier(), a.getExtension())));
                            }
                        }
                    });
                });
    }

    private void addDeploymentVariants() {
        final Map<String, List<ExtensionDependency<?>>> deploymentDeps = new HashMap<>();
        for (var satisfiedDeps : satisfiedExtensionDeps.values()) {
            project.getDependencies().getComponents().withModule(
                    satisfiedDeps.getModuleId(),
                    satisfiedDeps::addFinalConditionalVariants);
            satisfiedDeps.collectExtensionDeps(deploymentDeps);
        }

        for (var pd : processedDeps.values()) {
            pd.addDeploymentDependency(deploymentDeps);
        }

        for (var deployment : deploymentDeps.entrySet()) {
            addDeploymentVariant(deployment.getKey(), deployment.getValue());
        }
    }

    private void addDeploymentVariant(String parentModule, List<ExtensionDependency<?>> extDeps) {
        project.getDependencies().getComponents().withModule(
                parentModule,
                compDetails -> addDeploymentVariant(
                        getDeploymentDependencyAttribute(project.getName(), mode),
                        compDetails, extDeps));
    }

    private static void addDeploymentVariant(Attribute<String> attribute,
            ComponentMetadataDetails compDetails,
            List<ExtensionDependency<?>> deploymentDeps) {
        addDeploymentVariant(attribute, RUNTIME_ELEMENTS, compDetails, deploymentDeps);
        addDeploymentVariant(attribute, RUNTIME, compDetails, deploymentDeps);
    }

    private static void addDeploymentVariant(Attribute<String> attribute, String baseVariant,
            ComponentMetadataDetails compDetails,
            List<ExtensionDependency<?>> deploymentDeps) {
        compDetails.maybeAddVariant(attribute.getName(), baseVariant, variant -> {
            variant.attributes(attrs -> {
                attrs.attribute(attribute, ON);
            });
            variant.withDependencies(directDeps -> {
                for (var deploymentDep : deploymentDeps) {
                    boolean alreadyAdded = false;
                    for (var directDep : directDeps) {
                        if (directDep.getName().equals(deploymentDep.getDeploymentName()) &&
                                directDep.getGroup().equals(deploymentDep.getDeploymentGroup())) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        directDeps.add(deploymentDep.getDeploymentGroup() + ":" + deploymentDep.getDeploymentName() + ":"
                                + deploymentDep.getDeploymentVersion());
                    }
                }
            });
        });
    }

    private Configuration copyBaseConfig(Configuration baseConfig) {
        return project.getConfigurations()
                .resolvable(baseConfig.getName() + "Copy" + configCopyCounter.incrementAndGet(), c -> {
                    c.setCanBeConsumed(false);
                    c.extendsFrom(baseConfig);
                    setIterativeConditionalAttributes(c, project, satisfiedExtensionDeps.values());
                }).get();
    }

    private void processConfiguration(Configuration baseConfig) {
        var config = copyBaseConfig(baseConfig);
        final Set<ModuleVersionIdentifier> visited = new HashSet<>();
        for (var dep : config.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
            processDependency(null, dep, visited, true);
        }
    }

    private void processDependency(ProcessedDependency parent,
            ResolvedDependency dep,
            Set<ModuleVersionIdentifier> visited,
            boolean collectTopExtensions) {
        if (!visited.add(dep.getModule().getId())) {
            return;
        }
        var artifacts = dep.getModuleArtifacts();
        ProcessedDependency nextParent = null;
        if (!artifacts.isEmpty()) {
            for (var a : artifacts) {
                nextParent = processedDeps.computeIfAbsent(getKey(a), key -> {
                    var processedDep = new ProcessedDependency(a, DependencyUtils.getExtensionInfoOrNull(project, a),
                            a.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier);
                    processedDep.parent = parent;
                    processedDep.queueConditionalDeps();
                    return processedDep;
                });
                if (nextParent.extension != null) {
                    if (collectTopExtensions) {
                        nextParent.topLevelExt = true;
                        nextParent.parent = parent;
                        collectTopExtensions = false;
                    }
                }
            }
        }

        for (var c : dep.getChildren()) {
            processDependency(nextParent, c, visited, collectTopExtensions);
        }
    }

    private void queueConditionalDependency(ProcessedDependency parent, Dependency dep) {
        var conditionalDep = getOrCreateConditionalDep(dep);
        if (conditionalDep != null) {
            dependencyVariantQueue.add(new ConditionalDependencyVariant(parent.extension, conditionalDep));
        }
    }

    private ConditionalDependency getOrCreateConditionalDep(Dependency dep) {
        return allConditionalDeps.computeIfAbsent(
                ArtifactKey.of(dep.getGroup(), dep.getName(), dep.getVersion(), ArtifactCoords.TYPE_JAR),
                key -> newConditionalDep(dep));
    }

    private ResolvedArtifact tryResolvingRelocationArtifact(Dependency dep) {
        final Configuration configForRelocated = getDetachedWithExclusions(dep).setTransitive(true);
        setConditionalAttributes(configForRelocated, project, mode);

        var firstLevelDeps = configForRelocated.getResolvedConfiguration().getFirstLevelModuleDependencies();
        if (firstLevelDeps.size() != 1) {
            return null;
        }
        ResolvedDependency resolvedDep = firstLevelDeps.iterator().next();

        // If resolved dep is indeed relocated, it should have exactly one child and we assume
        // that child is the actual artifact we want.
        var childrenDeps = resolvedDep.getChildren();
        if (childrenDeps.size() != 1) {
            return null;
        }
        ResolvedDependency relocatedDep = childrenDeps.iterator().next();

        var resolvedArtifacts = relocatedDep.getModuleArtifacts();
        if (resolvedArtifacts.size() != 1) {
            return null;
        }

        ResolvedArtifact artifact = resolvedArtifacts.iterator().next();
        if (ArtifactCoords.TYPE_POM.equals(artifact.getExtension())) {
            return null;
        }

        return artifact;
    }

    private ConditionalDependency newConditionalDep(Dependency originalDep) {
        var dep = getConstrainedDep(originalDep);
        final Configuration config = getDetachedWithExclusions(dep).setTransitive(false);

        setConditionalAttributes(config, project, mode);
        ResolvedArtifact resolvedArtifact = null;

        for (var a : config.getResolvedConfiguration().getResolvedArtifacts()) {
            resolvedArtifact = a;
            break;
        }

        if (resolvedArtifact == null) {
            // likely a relocation artifact, in which case we want to resolve it with transitive deps and
            // take the first artifact.
            resolvedArtifact = tryResolvingRelocationArtifact(dep);
        }

        if (resolvedArtifact == null) {
            // check if that's due to exclude rules, and if yes, ignore
            if (isExplicitlyExcluded(dep)) {
                project.getLogger().info("Conditional dependency {} ignored due to exclusion rule", dep);
                return null;
            }
            throw new RuntimeException(dep + " did not resolve to any artifacts");
        }

        return new ConditionalDependency(
                getKey(resolvedArtifact),
                resolvedArtifact,
                DependencyUtils.getExtensionInfoOrNull(project, resolvedArtifact));

    }

    private boolean isExplicitlyExcluded(Dependency dep) {
        return platformSpecProperty.get().getExclusions().stream().anyMatch(rule -> {
            // Do not abort if the group is null, allow the next comparison to take place
            if (rule.getGroup() != null && !Objects.equals(rule.getGroup(), dep.getGroup())) {
                return false;
            }
            // If we reached this point, and module of the rule is null, it is a match
            return rule.getModule() == null || Objects.equals(rule.getModule(), dep.getName());
        });
    }

    private Configuration getDetachedWithExclusions(Dependency dep) {
        var c = project.getConfigurations().detachedConfiguration(dep);
        PlatformSpec platformSpec = platformSpecProperty.get();
        platformSpec.getExclusions().forEach(rule -> {
            Map<String, String> excludeProperties = new HashMap<>(2);
            excludeProperties.put("group", rule.getGroup());
            excludeProperties.put("module", rule.getModule());
            c.exclude(excludeProperties);
        });
        return c;
    }

    private Dependency getConstrainedDep(Dependency dep) {
        return findMatchingConstraint(dep).map(c -> project.getDependencies().create(
                dep.getGroup() + ":" + dep.getName() + ":" + c.getVersion())).orElse(dep);
    }

    private Optional<PlatformSpec.Constraint> findMatchingConstraint(Dependency dep) {
        PlatformSpec platformSpec = platformSpecProperty.get();
        Map<ArtifactKey, PlatformSpec.Constraint> constraints = platformSpec.getConstraints();
        PlatformSpec.Constraint matchingConstraint = constraints
                .get(ArtifactKey.ga(dep.getGroup(), dep.getName()));
        return Optional.ofNullable(matchingConstraint);
    }

    private class ProcessedDependency {
        private final ResolvedArtifact artifact;
        private final ExtensionDependency<?> extension;
        private final boolean local;
        private ProcessedDependency parent;
        private boolean topLevelExt;

        private ProcessedDependency(ResolvedArtifact artifact, ExtensionDependency<?> extension, boolean local) {
            this.artifact = artifact;
            this.extension = extension;
            this.local = local;
        }

        private void queueConditionalDeps() {
            if (extension == null) {
                return;
            }
            for (var dep : extension.getConditionalDependencies()) {
                queueConditionalDependency(this, dep);
            }
            if (mode == LaunchMode.DEVELOPMENT) {
                for (var dep : extension.getConditionalDevDependencies()) {
                    queueConditionalDependency(this, dep);
                }
            }
        }

        private void addDeploymentDependency(Map<String, List<ExtensionDependency<?>>> deploymentDeps) {
            if (extension == null || parent == null || parent.local || parent.extension != null && !topLevelExt) {
                return;
            }
            final String parentModule;
            if (parent.extension == null) {
                parentModule = parent.artifact.getModuleVersion().getId().getGroup() + ":"
                        + parent.artifact.getModuleVersion().getId().getName();
            } else {
                parentModule = parent.extension.getDeploymentGroup() + ":" + parent.extension.getDeploymentName();
            }
            deploymentDeps.computeIfAbsent(parentModule, k -> new ArrayList<>(1)).add(extension);
        }
    }

    private class ConditionalDependency {
        private final ArtifactKey key;
        private final ResolvedArtifact artifact;
        private final ExtensionDependency<?> extension;

        private ConditionalDependency(ArtifactKey key, ResolvedArtifact artifact, ExtensionDependency<?> extension) {
            this.key = key;
            this.artifact = artifact;
            this.extension = extension;
        }

        private boolean isConditionSatisfied() {
            if (extension == null || extension.getDependencyConditions().isEmpty()) {
                return true;
            }
            for (var key : extension.getDependencyConditions()) {
                if (!processedDeps.containsKey(key)) {
                    return false;
                }
            }
            return true;
        }
    }

    private record ConditionalDependencyVariant(ExtensionDependency<?> parent, ConditionalDependency conditionalDep) {
    }

    private class SatisfiedExtensionDeps {

        private final ExtensionDependency<?> parent;
        private final List<ConditionalDependency> deps = new ArrayList<>(4);
        private int depsInLastAddedVariant;
        private String iterativeVariantName;
        private int variantIteration;

        private SatisfiedExtensionDeps(ExtensionDependency<?> parent, String projectName, LaunchMode mode) {
            this.parent = parent;
            iterativeVariantName = "quarkus" +
                    ApplicationDeploymentClasspathBuilder.getLaunchModeAlias(mode) +
                    "Iterative" +
                    toUpperCaseName(projectName) +
                    toUpperCaseName(parent.getName());
        }

        private String getModuleId() {
            return parent.getGroup() + ":" + parent.getName();
        }

        private boolean hasNewDepsSinceLastAddedVariant() {
            return deps.size() > depsInLastAddedVariant;
        }

        private void setItrativeAttribute(AttributeContainer attrs) {
            /* @formatter:off
            It appears we can't re-use a previously added variant and with attribute.
            Somehow, it's not selected.

            String attrName = null;
            if (hasNewDepsSinceLastAddedVariant()) {
                // this method is called before addIterativeConditionalVariants
                attrName = iterativeVariantName + (variantIteration + 1);
            } else if (variantIteration > 0) {
                attrName = iterativeVariantName + variantIteration;
            }
            if (attrName != null) {
                attrs.attribute(Attribute.of(attrName, String.class), ON);
            }
            @formatter:on */
            // this method is called before addIterativeConditionalVariants
            attrs.attribute(Attribute.of(iterativeVariantName + (variantIteration + 1), String.class), ON);
        }

        private void addIterativeConditionalVariants(ComponentMetadataDetails compDetails) {
            // It appears we can't re-use a previously added variant and with attribute.
            // Somehow, it's not selected.
            //if (hasNewDepsSinceLastAddedVariant()) {
            final String variantName = iterativeVariantName + ++variantIteration;
            depsInLastAddedVariant = deps.size();
            final Attribute<String> attr = Attribute.of(variantName, String.class);
            project.getDependencies().getAttributesSchema().attribute(attr);
            addConditionalVariant(variantName, compDetails, attr, deps);
            //}
        }

        private void addFinalConditionalVariants(ComponentMetadataDetails compDetails) {
            addConditionalVariant(quarkusDepAttr.getName(), compDetails, quarkusDepAttr, deps);
        }

        private void collectExtensionDeps(Map<String, List<ExtensionDependency<?>>> extMap) {
            for (var dep : deps) {
                if (dep.extension != null) {
                    extMap.computeIfAbsent(parent.getDeploymentGroup() + ":" + parent.getDeploymentName(),
                            k -> new ArrayList<>(4)).add(dep.extension);
                }
            }
        }
    }
}
