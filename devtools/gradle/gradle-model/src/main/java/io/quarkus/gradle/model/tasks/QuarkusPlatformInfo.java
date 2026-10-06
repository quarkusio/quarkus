package io.quarkus.gradle.model.tasks;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Gradle-decorated nested input containing resolved Quarkus platform descriptors and properties.
 * <p>
 * Gradle fingerprints scalar artifact identity separately from file contents. Task execution retains the internal
 * resolved-artifact objects to translate JSON descriptors and properties artifacts into Quarkus platform imports.
 * Public visibility is required for Gradle decoration; this is not a user DSL type.
 */
public abstract class QuarkusPlatformInfo {

    static final String PLATFORM_DESCRIPTOR_ARTIFACT_TYPE = "json";
    static final String PLATFORM_PROPERTIES_ARTIFACT_TYPE = "properties";

    /** @return internal resolved artifacts consumed during task execution */
    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

    /** @return canonical scalar fingerprints of artifact identity, type, and path */
    @Input
    public abstract ListProperty<String> getArtifactMetadata();

    /** @return descriptor and properties files tracked with relative path sensitivity */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getArtifactFiles();

    PlatformImportsImpl resolvePlatformImports() {
        final PlatformImportsImpl result = new PlatformImportsImpl();
        for (var artifact : getResolvedArtifacts().get()) {
            var compId = ((ModuleComponentArtifactIdentifier) artifact.getId()).getComponentIdentifier();
            final String artifactId = artifact.getFile().getName();
            if (artifactId.endsWith(".json")) {
                result.addPlatformDescriptor(compId.getGroup(), compId.getModuleIdentifier().getName(),
                        compId.getVersion(), PLATFORM_DESCRIPTOR_ARTIFACT_TYPE, compId.getVersion());
            } else if (artifactId.endsWith(".properties")) {
                try {
                    result.addPlatformProperties(compId.getGroup(), compId.getModuleIdentifier().getName(),
                            ArtifactCoords.DEFAULT_CLASSIFIER, PLATFORM_PROPERTIES_ARTIFACT_TYPE, compId.getVersion(),
                            artifact.getFile().toPath());
                } catch (AppModelResolverException e) {
                    throw new RuntimeException("Failed to add platform properties " + artifact, e);
                }
            }
        }
        return result;
    }

    /**
     * Lazily wires this nested input to a resolvable Gradle configuration.
     *
     * @param configuration configuration containing platform JSON descriptors and properties artifacts
     */
    public void configureFrom(Configuration configuration) {
        var artifacts = configuration.getIncoming().getArtifacts();
        getResolvedArtifacts().set(artifacts.getResolvedArtifacts());
        getArtifactMetadata().set(artifacts.getResolvedArtifacts().map(ResolvedInputFingerprint::artifactMetadata));
        getArtifactFiles().from(artifacts.getArtifactFiles());
    }
}
