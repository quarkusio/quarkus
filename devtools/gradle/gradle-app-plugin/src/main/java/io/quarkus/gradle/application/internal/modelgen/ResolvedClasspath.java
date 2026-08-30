package io.quarkus.gradle.application.internal.modelgen;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import io.quarkus.gradle.model.tasks.ResolvedInputFingerprint;

/**
 * Lazy resolution-result input for application model generation.
 */
public abstract class ResolvedClasspath {

    @Internal
    public abstract Property<ResolvedComponentResult> getRoot();

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResolvedArtifactFiles();

    @Input
    public abstract ListProperty<String> getResolutionGraph();

    @Input
    public abstract ListProperty<String> getArtifactMetadata();

    @Internal
    FileCollection getAllResolvedFiles() {
        return getResolvedArtifactFiles();
    }

    Map<ComponentIdentifier, List<ResolvedArtifact>> resolvedArtifactsByComponentIdentifier() {
        return getQuarkusResolvedArtifacts().stream()
                .collect(Collectors.groupingBy(artifact -> artifact.id.getComponentIdentifier()));
    }

    private List<ResolvedArtifact> getQuarkusResolvedArtifacts() {
        return getResolvedArtifacts().get().stream()
                .map(this::toResolvedArtifact)
                .collect(toList());
    }

    private ResolvedArtifact toResolvedArtifact(ResolvedArtifactResult result) {
        String type = result.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
        return new ResolvedArtifact(result.getId(), result.getFile(), type);
    }

    public void configureFrom(Configuration configuration) {
        ResolvableDependencies resolvableDependencies = configuration.getIncoming();
        var root = resolvableDependencies.getResolutionResult().getRootComponent();
        getRoot().set(root);
        getResolutionGraph().set(root.map(ResolvedInputFingerprint::resolutionGraph));
        var artifacts = resolvableDependencies.getArtifacts();
        getResolvedArtifacts().set(artifacts.getResolvedArtifacts());
        getArtifactMetadata().set(artifacts.getResolvedArtifacts().map(ResolvedClasspath::artifactMetadata));
        getResolvedArtifactFiles().from(artifacts.getArtifactFiles());
    }

    public static List<String> artifactMetadata(Set<ResolvedArtifactResult> artifacts) {
        return ResolvedInputFingerprint.artifactMetadata(artifacts);
    }
}
