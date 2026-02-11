package io.quarkus.gradle.tooling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
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
import org.jspecify.annotations.Nullable;

public class GradleAssistedMavenModelResolverImpl implements ModelResolver {

    private final Project project;
    private final Map<String, File> pomCache = new ConcurrentHashMap<>();

    public GradleAssistedMavenModelResolverImpl(Project project) {
        this.project = project;
    }

    private GradleAssistedMavenModelResolverImpl(GradleAssistedMavenModelResolverImpl other) {
        this.project = other.project;
        this.pomCache.putAll(other.pomCache);
    }

    private static String cacheKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public ModelSource2 resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        String key = cacheKey(groupId, artifactId, version);

        File pomFile = pomCache.get(key);
        if (pomFile == null) {
            pomFile = resolvePomViaQuery(groupId, artifactId, version);
            if (pomFile != null) {
                pomCache.put(key, pomFile);
            }
        }

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
            public @Nullable ModelSource2 getRelatedSource(String relPath) {
                return null;
            }

            @Override
            public URI getLocationURI() {
                return resolvedPom.toURI();
            }
        };
    }

    private @Nullable File resolvePomViaQuery(String groupId, String artifactId, String version) {
        @SuppressWarnings("unchecked")
        var componentId = project.getDependencies()
                .createArtifactResolutionQuery()
                .forModule(groupId, artifactId, version)
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        for (var component : componentId.getResolvedComponents()) {
            for (var artifactResult : component.getArtifacts(MavenPomArtifact.class)) {
                if (artifactResult instanceof ResolvedArtifactResult resolved) {
                    return resolved.getFile();
                }
            }
        }
        return null;
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
        return new GradleAssistedMavenModelResolverImpl(this);
    }
}