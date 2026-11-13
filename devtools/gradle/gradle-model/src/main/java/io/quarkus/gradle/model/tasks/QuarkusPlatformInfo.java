package io.quarkus.gradle.model.tasks;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactCoords;

public abstract class QuarkusPlatformInfo {

    static final String PLATFORM_DESCRIPTOR_ARTIFACT_TYPE = "json";
    static final String PLATFORM_PROPERTIES_ARTIFACT_TYPE = "properties";

    /**
     * Internal since we track defined dependencies via {@link QuarkusApplicationModelTask#getOriginalClasspath}
     */
    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

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

    public void configureFrom(Configuration configuration) {
        getResolvedArtifacts().set(configuration.getIncoming().getArtifacts().getResolvedArtifacts());
    }
}
