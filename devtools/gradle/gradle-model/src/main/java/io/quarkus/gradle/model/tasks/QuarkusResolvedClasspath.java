package io.quarkus.gradle.model.tasks;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
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

/**
 * Gradle-decorated nested input for a resolved classpath consumed by application-model generation.
 * <p>
 * Gradle cannot directly fingerprint resolution-result object graphs, so the graph and artifact identities are exposed
 * as canonical scalar fingerprints while artifact contents are declared separately as files. The internal Gradle
 * result objects remain lazy until task execution. Public visibility supports Gradle decoration and shared plugin code;
 * this is not application DSL.
 *
 * @see <a href="https://docs.gradle.org/current/samples/sample_tasks_with_dependency_resolution_result_inputs.html">
 *      Gradle's dependency-resolution task-input sample</a>
 */
public abstract class QuarkusResolvedClasspath {

    /** @return the internal lazy root component of the resolution graph */
    @Internal
    public abstract Property<ResolvedComponentResult> getRoot();

    /** @return the internal lazy resolved artifacts consumed during model assembly */
    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

    /** @return resolved artifact files tracked with relative path sensitivity */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResolvedArtifactFiles();

    /** @return canonical scalar representation of resolved and unresolved graph edges */
    @Input
    public abstract ListProperty<String> getResolutionGraph();

    /** @return canonical scalar fingerprints of resolved artifact identity, type, and path */
    @Input
    public abstract ListProperty<String> getArtifactMetadata();

    @Internal
    FileCollection getAllResolvedFiles() {
        return getResolvedArtifactFiles();
    }

    Map<ComponentIdentifier, List<QuarkusResolvedArtifact>> resolvedArtifactsByComponentIdentifier() {
        return getQuarkusResolvedArtifacts().stream()
                .collect(Collectors.groupingBy(artifact -> artifact.id.getComponentIdentifier()));
    }

    private List<QuarkusResolvedArtifact> getQuarkusResolvedArtifacts() {
        return getResolvedArtifacts().get().stream()
                .map(this::toResolvedArtifact)
                .collect(toList());
    }

    private QuarkusResolvedArtifact toResolvedArtifact(ResolvedArtifactResult result) {
        String type = result.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
        return new QuarkusResolvedArtifact(result.getId(), result.getFile(), type);
    }

    /**
     * Lazily wires all graph, artifact, and file inputs from {@code configuration}.
     *
     * @param configuration resolvable classpath configuration
     */
    public void configureFrom(Configuration configuration) {
        ResolvableDependencies resolvableDependencies = configuration.getIncoming();
        var root = resolvableDependencies.getResolutionResult().getRootComponent();
        getRoot().set(root);
        getResolutionGraph().set(root.map(ResolvedInputFingerprint::resolutionGraph));
        var artifacts = resolvableDependencies.getArtifacts();
        getResolvedArtifacts().set(artifacts.getResolvedArtifacts());
        getArtifactMetadata().set(artifacts.getResolvedArtifacts().map(ResolvedInputFingerprint::artifactMetadata));
        getResolvedArtifactFiles().from(artifacts.getArtifactFiles());
    }

}
