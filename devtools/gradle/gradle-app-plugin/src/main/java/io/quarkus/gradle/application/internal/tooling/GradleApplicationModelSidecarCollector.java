package io.quarkus.gradle.application.internal.tooling;

import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Kind.CLASSES;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Kind.PROCESSED_RESOURCES;
import static io.quarkus.bootstrap.model.gradle.GradleModelAssociation.Kind.APPLICATION;
import static io.quarkus.bootstrap.model.gradle.GradleModelAssociation.Kind.DEPENDENCY;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.COMPILE_ONLY;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.DEPLOYMENT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.RUNTIME;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.TEST;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.DIRECT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.TRANSITIVE;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.WORKSPACE_DIRECT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.EXTENSION_DEPLOYMENT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.EXTENSION_RUNTIME;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.WORKSPACE_DEPENDENCY;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleModelAssociation;
import io.quarkus.bootstrap.model.gradle.GradleModelCorrelationSupport;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.GradleProjectIdentity;
import io.quarkus.bootstrap.model.gradle.GradleSourceObservation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleModelAssociation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleModelCorrelation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleProjectIdentity;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleSourceObservation;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews.ModeViews;
import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.LaunchMode;

/**
 * Copies Gradle resolution facts into the transport-neutral sidecar.
 */
final class GradleApplicationModelSidecarCollector {

    private final Project project;
    private final ModeViews views;
    private final Map<ProjectComponentIdentifier, MutableProjectComponent> components = new LinkedHashMap<>();
    private final Map<ProjectComponentIdentifier, ModuleCoordinates> coordinates = new HashMap<>();

    GradleApplicationModelSidecarCollector(Project project, ModeViews views) {
        this.project = Objects.requireNonNull(project, "project");
        this.views = Objects.requireNonNull(views, "views");
    }

    GradleApplicationModelSidecar collect(LaunchMode mode, ApplicationModel applicationModel) {
        ResolvedComponentResult runtimeRoot = root(views.runtime());
        if (!(runtimeRoot.getId() instanceof ProjectComponentIdentifier targetIdentifier)) {
            throw new IllegalStateException("The application model target is not a Gradle project component: "
                    + runtimeRoot.getId().getDisplayName());
        }

        MutableProjectComponent target = component(targetIdentifier);
        target.roles.add(GradleProjectComponent.Role.APPLICATION);
        target.classpaths.add(RUNTIME);
        if (mode == LaunchMode.TEST) {
            target.classpaths.add(TEST);
        }
        rememberCoordinates(runtimeRoot);

        collectGraph(views.runtime(), RUNTIME, mode == LaunchMode.TEST);
        collectGraph(views.deployment(), DEPLOYMENT, mode == LaunchMode.TEST);
        collectGraph(views.compileOnly(), COMPILE_ONLY, mode == LaunchMode.TEST);

        // collect() runs synchronously only while Gradle serves this Tooling API
        // model. Realizing these providers here must not move into plugin application.
        Set<ResolvedArtifactResult> classOutputs = views.localOutputs().classArtifacts().get();
        Set<ResolvedArtifactResult> resourceOutputs = views.localOutputs().resourceArtifacts().get();
        Map<OutputOccurrenceKey, Integer> outputOccurrences = outputOccurrences(classOutputs, CLASSES,
                resourceOutputs, PROCESSED_RESOURCES);
        collectResolvedOutputs(classOutputs, CLASSES, applicationModel,
                outputOccurrences,
                targetIdentifier);
        collectResolvedOutputs(resourceOutputs, PROCESSED_RESOURCES, applicationModel,
                outputOccurrences,
                targetIdentifier);
        collectResolvedSources(views.mainSources().getArtifacts().getResolvedArtifacts().get());
        collectExtensionRuntimeRoles(views.runtime().getIncoming().getArtifacts().getArtifacts());
        collectExtensionDeploymentRoles(views.deploymentMarkers().getArtifacts().getResolvedArtifacts().get());
        collectTargetProject(targetIdentifier, mode, applicationModel);
        applyApplicationModelRoles(applicationModel);

        // Tooling models cross a serialization boundary. Canonical ordering keeps
        // equivalent resolution facts deterministic for consumers and correlation.
        List<GradleProjectComponent> result = components.values().stream()
                .map(MutableProjectComponent::toModel)
                .sorted(Comparator.comparing(component -> component.getProjectIdentity().getBuildTreePath()))
                .toList();
        GradleProjectIdentity targetIdentity = identity(targetIdentifier);
        GradleApplicationModelSidecar.Mode sidecarMode = GradleApplicationModelSidecar.Mode.valueOf(mode.name());
        return new DefaultGradleApplicationModelSidecar(
                new DefaultGradleModelCorrelation(
                        GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION,
                        sidecarMode,
                        targetIdentity.getBuildTreePath(),
                        GradleModelCorrelationSupport.canonicalGraphFacts(applicationModel)),
                targetIdentity,
                result);
    }

    private void collectGraph(Configuration configuration,
            GradleProjectComponent.ClasspathAssociation association,
            boolean testMode) {
        ResolvedComponentResult root = root(configuration);
        rememberCoordinates(root);
        Set<ComponentIdentifier> visited = new LinkedHashSet<>();
        ArrayDeque<GraphNode> queue = new ArrayDeque<>();
        for (DependencyResult dependency : root.getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult resolved) {
                queue.addLast(new GraphNode(resolved.getSelected(), true));
            }
        }
        while (!queue.isEmpty()) {
            GraphNode node = queue.removeFirst();
            ResolvedComponentResult selected = node.component();
            rememberCoordinates(selected);
            if (selected.getId() instanceof ProjectComponentIdentifier identifier) {
                MutableProjectComponent component = component(identifier);
                component.roles.add(WORKSPACE_DEPENDENCY);
                component.classpaths.add(association);
                if (testMode) {
                    component.classpaths.add(TEST);
                }
                component.relationships.add(node.direct() ? DIRECT : TRANSITIVE);
            }
            if (!visited.add(selected.getId())) {
                continue;
            }
            for (DependencyResult dependency : selected.getDependencies()) {
                if (dependency instanceof ResolvedDependencyResult resolved) {
                    queue.addLast(new GraphNode(resolved.getSelected(), false));
                }
            }
        }
    }

    private void collectResolvedOutputs(Set<ResolvedArtifactResult> artifacts, GradleLogicalOutput.Kind kind,
            ApplicationModel applicationModel, Map<OutputOccurrenceKey, Integer> outputOccurrences,
            ProjectComponentIdentifier targetIdentifier) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
            if (!(componentIdentifier instanceof ProjectComponentIdentifier projectIdentifier)) {
                continue;
            }
            MutableProjectComponent component = component(projectIdentifier);
            component.roles.add(WORKSPACE_DEPENDENCY);
            Path path = artifact.getFile().toPath();
            String selectedArtifactIdentity = selectedArtifactIdentity(artifact);
            String outputIdentity = outputIdentity(projectIdentifier, kind, selectedArtifactIdentity, path);
            component.outputs.add(new DefaultGradleLogicalOutput(
                    outputIdentity,
                    kind,
                    GradleLogicalOutput.Scope.UNKNOWN,
                    path.toString(),
                    selectedArtifactIdentity,
                    null,
                    null,
                    null,
                    null,
                    GradleLogicalOutput.Materialization.UNKNOWN,
                    GradleLogicalOutput.ProducerCategory.UNKNOWN,
                    modelAssociation(projectIdentifier, targetIdentifier, selectedArtifactIdentity, kind, path,
                            outputOccurrences, applicationModel)));
        }
    }

    private static Map<OutputOccurrenceKey, Integer> outputOccurrences(
            Set<ResolvedArtifactResult> classOutputs, GradleLogicalOutput.Kind classKind,
            Set<ResolvedArtifactResult> resourceOutputs, GradleLogicalOutput.Kind resourceKind) {
        Map<OutputOccurrenceKey, Integer> occurrences = new HashMap<>();
        countOutputOccurrences(classOutputs, classKind, occurrences);
        countOutputOccurrences(resourceOutputs, resourceKind, occurrences);
        return occurrences;
    }

    private static void countOutputOccurrences(Set<ResolvedArtifactResult> artifacts, GradleLogicalOutput.Kind kind,
            Map<OutputOccurrenceKey, Integer> occurrences) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
            if (identifier instanceof ProjectComponentIdentifier projectIdentifier) {
                OutputOccurrenceKey key = new OutputOccurrenceKey(projectIdentifier, kind,
                        normalized(artifact.getFile().toPath()));
                occurrences.merge(key, 1, Integer::sum);
            }
        }
    }

    private static String selectedArtifactIdentity(ResolvedArtifactResult artifact) {
        String capabilities = artifact.getVariant().getCapabilities().stream()
                .map(capability -> capability.getGroup() + ':' + capability.getName() + ':' + capability.getVersion())
                .sorted()
                .reduce((left, right) -> left + ',' + right)
                .orElse("");
        return artifact.getId().getDisplayName() + "|capabilities=" + capabilities;
    }

    private void collectResolvedSources(Set<ResolvedArtifactResult> artifacts) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
            if (!(componentIdentifier instanceof ProjectComponentIdentifier projectIdentifier)) {
                continue;
            }
            MutableProjectComponent component = component(projectIdentifier);
            component.roles.add(WORKSPACE_DEPENDENCY);
            component.sources.add(new DefaultGradleSourceObservation(
                    artifact.getFile().toPath().toString(),
                    GradleSourceObservation.Role.UNKNOWN,
                    null));
        }
    }

    private void collectExtensionRuntimeRoles(Set<ResolvedArtifactResult> artifacts) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
            Boolean extensionRuntime = artifact.getVariant().getAttributes()
                    .getAttribute(ExtensionVariantConstants.EXTENSION_RUNTIME_ATTRIBUTE);
            if (identifier instanceof ProjectComponentIdentifier projectIdentifier
                    && Boolean.TRUE.equals(extensionRuntime)) {
                component(projectIdentifier).roles.add(EXTENSION_RUNTIME);
            }
        }
    }

    private void collectExtensionDeploymentRoles(Set<ResolvedArtifactResult> artifacts) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
            if (identifier instanceof ProjectComponentIdentifier projectIdentifier) {
                component(projectIdentifier).roles.add(EXTENSION_DEPLOYMENT);
            }
        }
    }

    private void collectTargetProject(ProjectComponentIdentifier targetIdentifier, LaunchMode mode,
            ApplicationModel applicationModel) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        List<SourceSet> includedSourceSets = new ArrayList<>();
        includedSourceSets.add(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
        if (mode == LaunchMode.TEST) {
            includedSourceSets.add(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME));
        }
        Map<OutputOccurrenceKey, Integer> outputOccurrences = targetOutputOccurrences(targetIdentifier,
                includedSourceSets);

        addTargetSourceSet(component(targetIdentifier), targetIdentifier,
                includedSourceSets.get(0), outputOccurrences, applicationModel);
        if (mode == LaunchMode.TEST) {
            SourceSet testSourceSet = includedSourceSets.get(1);
            addTargetSourceSet(component(targetIdentifier), targetIdentifier, testSourceSet, outputOccurrences,
                    applicationModel);
            addTargetSourceObservations(component(targetIdentifier), testSourceSet);
        }

        Configuration mainSources = project.getConfigurations().findByName(SourceSet.MAIN_SOURCE_SET_NAME + "SourceElements");
        if (mainSources != null) {
            for (PublishArtifact artifact : mainSources.getOutgoing().getArtifacts()) {
                component(targetIdentifier).sources.add(new DefaultGradleSourceObservation(
                        artifact.getFile().toPath().toString(),
                        GradleSourceObservation.Role.UNKNOWN,
                        null));
            }
        } else {
            addTargetSourceObservations(component(targetIdentifier),
                    sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
        }
    }

    private static Map<OutputOccurrenceKey, Integer> targetOutputOccurrences(
            ProjectComponentIdentifier targetIdentifier, List<SourceSet> sourceSets) {
        Map<OutputOccurrenceKey, Integer> occurrences = new HashMap<>();
        for (SourceSet sourceSet : sourceSets) {
            for (File file : sourceSet.getOutput().getClassesDirs().getFiles()) {
                occurrences.merge(new OutputOccurrenceKey(targetIdentifier, CLASSES, normalized(file.toPath())),
                        1, Integer::sum);
            }
            File resources = sourceSet.getOutput().getResourcesDir();
            if (resources != null) {
                occurrences.merge(
                        new OutputOccurrenceKey(targetIdentifier, PROCESSED_RESOURCES,
                                normalized(resources.toPath())),
                        1, Integer::sum);
            }
        }
        return occurrences;
    }

    private static void addTargetSourceObservations(MutableProjectComponent component, SourceSet sourceSet) {
        for (File sourceDirectory : sourceSet.getAllSource().getSrcDirs()) {
            component.sources.add(new DefaultGradleSourceObservation(
                    sourceDirectory.toPath().toString(),
                    GradleSourceObservation.Role.UNKNOWN,
                    null));
        }
    }

    private void addTargetSourceSet(MutableProjectComponent component, ProjectComponentIdentifier targetIdentifier,
            SourceSet sourceSet, Map<OutputOccurrenceKey, Integer> outputOccurrences,
            ApplicationModel applicationModel) {
        for (File file : sourceSet.getOutput().getClassesDirs().getFiles()) {
            addTargetOutput(component, targetIdentifier, sourceSet.getName(), CLASSES, file.toPath(),
                    outputOccurrences, applicationModel);
        }
        File resources = sourceSet.getOutput().getResourcesDir();
        if (resources != null) {
            addTargetOutput(component, targetIdentifier, sourceSet.getName(), PROCESSED_RESOURCES, resources.toPath(),
                    outputOccurrences, applicationModel);
        }
    }

    private void addTargetOutput(MutableProjectComponent component, ProjectComponentIdentifier targetIdentifier,
            String sourceSet, GradleLogicalOutput.Kind kind, Path path,
            Map<OutputOccurrenceKey, Integer> outputOccurrences, ApplicationModel applicationModel) {
        String artifactIdentity = "target:" + targetIdentifier.getBuildTreePath() + ':' + sourceSet + ':' + kind;
        component.outputs.add(new DefaultGradleLogicalOutput(
                outputIdentity(targetIdentifier, kind, artifactIdentity, path),
                kind,
                SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet)
                        ? GradleLogicalOutput.Scope.TEST
                        : GradleLogicalOutput.Scope.MAIN,
                path.toString(),
                artifactIdentity,
                null,
                sourceSet,
                null,
                null,
                GradleLogicalOutput.Materialization.UNKNOWN,
                GradleLogicalOutput.ProducerCategory.UNKNOWN,
                modelAssociation(targetIdentifier, targetIdentifier, artifactIdentity, kind, path,
                        outputOccurrences,
                        applicationModel)));
    }

    private void applyApplicationModelRoles(ApplicationModel applicationModel) {
        Set<ModuleCoordinates> applicationWorkspaceDependencies = new LinkedHashSet<>();
        if (applicationModel.getApplicationModule() != null) {
            for (Dependency dependency : applicationModel.getApplicationModule().getDirectDependencies()) {
                applicationWorkspaceDependencies.add(ModuleCoordinates.of(dependency));
            }
        }
        for (Map.Entry<ProjectComponentIdentifier, MutableProjectComponent> entry : components.entrySet()) {
            if (entry.getValue().roles.contains(GradleProjectComponent.Role.APPLICATION)) {
                continue;
            }
            ModuleCoordinates componentCoordinates = coordinates.get(entry.getKey());
            if (componentCoordinates == null || hasComponentCoordinateCollision(componentCoordinates)) {
                continue;
            }
            if (applicationWorkspaceDependencies.contains(componentCoordinates)) {
                entry.getValue().relationships.add(WORKSPACE_DIRECT);
            }
        }
    }

    private GradleModelAssociation modelAssociation(ProjectComponentIdentifier projectIdentifier,
            ProjectComponentIdentifier targetIdentifier, String selectedArtifactIdentity,
            GradleLogicalOutput.Kind outputKind, Path output,
            Map<OutputOccurrenceKey, Integer> outputOccurrences, ApplicationModel applicationModel) {
        // Never guess an application-model occurrence. Duplicate logical outputs,
        // composite coordinate collisions, or multiple path matches all degrade to
        // UNKNOWN so consumers cannot replace the wrong model path.
        if (selectedArtifactIdentity.isBlank()
                || outputOccurrences.getOrDefault(
                        new OutputOccurrenceKey(projectIdentifier, outputKind, normalized(output)), 0) != 1) {
            return DefaultGradleModelAssociation.unknown();
        }
        Path normalizedOutput = normalized(output);
        List<ModelOccurrence> candidates = new ArrayList<>();
        if (projectIdentifier.equals(targetIdentifier)) {
            addMatchingOccurrences(candidates, APPLICATION, applicationModel.getAppArtifact(), normalizedOutput);
        } else {
            ModuleCoordinates projectCoordinates = coordinates.get(projectIdentifier);
            if (projectCoordinates == null || hasComponentCoordinateCollision(projectCoordinates)) {
                return DefaultGradleModelAssociation.unknown();
            }
            for (ResolvedDependency dependency : applicationModel.getDependencies()) {
                if (projectCoordinates.equals(ModuleCoordinates.of(dependency))) {
                    addMatchingOccurrences(candidates, DEPENDENCY, dependency, normalizedOutput);
                }
            }
        }
        if (candidates.size() != 1) {
            return DefaultGradleModelAssociation.unknown();
        }
        ModelOccurrence occurrence = candidates.get(0);
        return new DefaultGradleModelAssociation(occurrence.kind(), occurrence.coordinates(),
                occurrence.path(), occurrence.classifier());
    }

    private boolean hasComponentCoordinateCollision(ModuleCoordinates candidateCoordinates) {
        int matches = 0;
        for (Map.Entry<ProjectComponentIdentifier, ModuleCoordinates> entry : coordinates.entrySet()) {
            if (candidateCoordinates.equals(entry.getValue())
                    && ++matches > 1) {
                return true;
            }
        }
        return false;
    }

    private static void addMatchingOccurrences(List<ModelOccurrence> target,
            GradleModelAssociation.Kind kind, ResolvedDependency dependency, Path normalizedOutput) {
        PathCollection resolvedPaths = dependency.getResolvedPaths();
        if (resolvedPaths == null) {
            return;
        }
        for (Path path : resolvedPaths) {
            if (normalized(path).equals(normalizedOutput)) {
                target.add(new ModelOccurrence(kind, dependency.toGACTVString(), path.toString(),
                        dependency.getClassifier()));
            }
        }
    }

    private void rememberCoordinates(ResolvedComponentResult component) {
        if (!(component.getId() instanceof ProjectComponentIdentifier projectIdentifier)) {
            return;
        }
        ModuleVersionIdentifier moduleVersion = component.getModuleVersion();
        if (moduleVersion != null) {
            coordinates.put(projectIdentifier, ModuleCoordinates.of(moduleVersion));
        }
    }

    private MutableProjectComponent component(ProjectComponentIdentifier identifier) {
        return components.computeIfAbsent(identifier, ignored -> new MutableProjectComponent(identifier));
    }

    private static ResolvedComponentResult root(Configuration configuration) {
        return configuration.getIncoming().getResolutionResult().getRootComponent().get();
    }

    private static String outputIdentity(ProjectComponentIdentifier identifier, GradleLogicalOutput.Kind kind,
            String artifactIdentity, Path path) {
        return identifier.getBuildTreePath() + '|' + kind + '|' + artifactIdentity + '|' + path;
    }

    private static Path normalized(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static GradleProjectIdentity identity(ProjectComponentIdentifier identifier) {
        return new DefaultGradleProjectIdentity(
                identifier.getBuild().getBuildPath(),
                identifier.getProjectPath(),
                identifier.getBuildTreePath());
    }

    private static final class MutableProjectComponent {

        private final ProjectComponentIdentifier identifier;
        private final Set<GradleProjectComponent.Role> roles = EnumSet.noneOf(GradleProjectComponent.Role.class);
        private final Set<GradleProjectComponent.ClasspathAssociation> classpaths = EnumSet
                .noneOf(GradleProjectComponent.ClasspathAssociation.class);
        private final Set<GradleProjectComponent.GraphRelationship> relationships = EnumSet
                .noneOf(GradleProjectComponent.GraphRelationship.class);
        private final List<GradleSourceObservation> sources = new ArrayList<>();
        private final List<GradleLogicalOutput> outputs = new ArrayList<>();

        private MutableProjectComponent(ProjectComponentIdentifier identifier) {
            this.identifier = identifier;
        }

        private GradleProjectComponent toModel() {
            sources.sort(Comparator.comparing(GradleSourceObservation::getPath));
            outputs.sort(Comparator.comparing(GradleLogicalOutput::getIdentity));
            return new DefaultGradleProjectComponent(
                    identity(identifier),
                    identifier.getDisplayName(),
                    roles,
                    classpaths,
                    relationships,
                    sources,
                    outputs);
        }
    }

    private record GraphNode(ResolvedComponentResult component, boolean direct) {
    }

    private record ModelOccurrence(GradleModelAssociation.Kind kind, String coordinates, String path,
            String classifier) {
    }

    private record OutputOccurrenceKey(ProjectComponentIdentifier component, GradleLogicalOutput.Kind kind, Path path) {
    }

    private record ModuleCoordinates(String group, String name, String version) {

        private static ModuleCoordinates of(ModuleVersionIdentifier identifier) {
            return new ModuleCoordinates(identifier.getGroup(), identifier.getName(), identifier.getVersion());
        }

        private static ModuleCoordinates of(ResolvedDependency dependency) {
            return new ModuleCoordinates(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        private static ModuleCoordinates of(Dependency dependency) {
            return new ModuleCoordinates(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }
    }
}
