package io.quarkus.gradle.application.internal.modelgen;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactCoords;

public abstract class PlatformInfo {

    static final String PLATFORM_DESCRIPTOR_ARTIFACT_TYPE = "json";
    static final String PLATFORM_PROPERTIES_ARTIFACT_TYPE = "properties";

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResolvedArtifactFiles();

    @Input
    public abstract ListProperty<String> getArtifactMetadata();

    PlatformImportsImpl resolvePlatformImports() {
        final PlatformImportsImpl result = new PlatformImportsImpl();
        for (var artifact : getResolvedArtifacts().get()) {
            if (!(artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier compId)) {
                continue;
            }
            final String artifactId = artifact.getFile().getName();
            if (artifactId.endsWith(".json")) {
                result.addPlatformDescriptor(compId.getGroup(), compId.getModule(),
                        compId.getVersion(), PLATFORM_DESCRIPTOR_ARTIFACT_TYPE, compId.getVersion());
            } else if (artifactId.endsWith(".properties")) {
                try {
                    result.addPlatformProperties(compId.getGroup(), compId.getModule(),
                            ArtifactCoords.DEFAULT_CLASSIFIER, PLATFORM_PROPERTIES_ARTIFACT_TYPE, compId.getVersion(),
                            artifact.getFile().toPath());
                } catch (AppModelResolverException e) {
                    throw new RuntimeException("Failed to add platform properties " + artifact, e);
                }
            }
        }
        return result;
    }

    public void configureFrom(Configuration configuration) {
        var artifacts = configuration.getIncoming().getArtifacts();
        getResolvedArtifacts().set(artifacts.getResolvedArtifacts());
        getResolvedArtifactFiles().from(artifacts.getArtifactFiles());
        getArtifactMetadata().set(artifacts.getResolvedArtifacts().map(ResolvedClasspath::artifactMetadata));
    }
}
