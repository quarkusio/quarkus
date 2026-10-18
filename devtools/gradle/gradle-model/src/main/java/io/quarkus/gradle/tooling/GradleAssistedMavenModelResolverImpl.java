package io.quarkus.gradle.tooling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;

import io.quarkus.maven.dependency.GAV;

public class GradleAssistedMavenModelResolverImpl implements ModelResolver {

    private final Project project;
    private final Map<GAV, Optional<File>> pomCache = new ConcurrentHashMap<>();

    public GradleAssistedMavenModelResolverImpl(Project project) {
        this.project = project;
    }

    private static GAV cacheKey(String groupId, String artifactId, String version) {
        return new GAV(groupId, artifactId, version);
    }

    @Override
    public ModelSource2 resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        GAV key = cacheKey(groupId, artifactId, version);

        File pomFile = pomCache
                .computeIfAbsent(key, this::resolvePomViaQuery)
                .orElse(null);

        if (pomFile == null) {
            throw new UnresolvableModelException(
                    "Could not resolve POM for " + groupId + ":" + artifactId + ":" + version,
                    groupId, artifactId, version);
        }

        final File resolvedPom = pomFile;
        return new ModelSource2() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(resolvedPom);
            }

            @Override
            public String getLocation() {
                return resolvedPom.getAbsolutePath();
            }

            @Override
            public ModelSource2 getRelatedSource(String relPath) {
                return null;
            }

            @Override
            public URI getLocationURI() {
                return resolvedPom.toURI();
            }
        };
    }

    private Optional<File> resolvePomViaQuery(GAV gav) {
        @SuppressWarnings("unchecked")
        var componentId = project.getDependencies()
                .createArtifactResolutionQuery()
                .forModule(gav.getGroupId(), gav.getArtifactId(), gav.getVersion())
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        for (var component : componentId.getResolvedComponents()) {
            for (var artifactResult : component.getArtifacts(MavenPomArtifact.class)) {
                if (artifactResult instanceof ResolvedArtifactResult resolved) {
                    return Optional.of(resolved.getFile());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
        // ignore
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        // ignore
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}
