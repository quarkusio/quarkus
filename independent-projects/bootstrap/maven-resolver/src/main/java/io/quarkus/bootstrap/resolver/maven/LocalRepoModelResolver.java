package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ResolvedDependency;

class LocalRepoModelResolver implements ModelResolver, LocalPomResolver {

    static LocalRepoModelResolver of(ApplicationModel appModel) {
        Map<String, LocalPomResolver> localRepoPaths = new HashMap<>(2);
        for (var d : appModel.getDependencies()) {
            addLocalRepoPath(d, localRepoPaths);
        }
        return of(localRepoPaths.values().toArray(new LocalPomResolver[0]));
    }

    private static void addLocalRepoPath(ResolvedDependency dep, Map<String, LocalPomResolver> collectedRepoPaths) {
        if (dep.getResolvedPaths().size() != 1) {
            return;
        }
        var artifactPath = dep.getResolvedPaths().getSinglePath();
        if (artifactPath.getNameCount() < 3) {
            return;
        }

        final String relativeArtifactDir = dep.getGroupId().replace('.', File.separatorChar) + File.separator
                + dep.getArtifactId();
        final String fullPath = artifactPath.toString();
        final int i = artifactPath.toString().indexOf(relativeArtifactDir);
        if (i < 0) {
            return;
        }
        final String repoDir = fullPath.substring(0, i);
        if (collectedRepoPaths.containsKey(repoDir)) {
            return;
        }

        // if the parent directory matches the version, assume it's the default Maven repository layout
        if (dep.getVersion().equals(artifactPath.getName(artifactPath.getNameCount() - 2).toString())) {
            collectedRepoPaths.put(repoDir, new MavenLocalPomResolver(new File(repoDir)));
        } else if (dep.getVersion().equals(artifactPath.getName(artifactPath.getNameCount() - 3).toString())) {
            collectedRepoPaths.put(repoDir, new GradleLocalPomResolver(new File(repoDir)));
        }
    }

    static LocalRepoModelResolver of(LocalPomResolver... pomResolvers) {
        return new LocalRepoModelResolver(List.of(pomResolvers));
    }

    private final List<LocalPomResolver> pomResolvers;

    LocalRepoModelResolver(List<LocalPomResolver> pomResolvers) {
        this.pomResolvers = pomResolvers;
    }

    @Override
    public File resolvePom(String groupId, String artifactId, String version) {
        for (LocalPomResolver pomResolver : pomResolvers) {
            var pom = pomResolver.resolvePom(groupId, artifactId, version);
            if (pom != null) {
                return pom;
            }
        }
        return null;
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        var pomXml = resolvePom(groupId, artifactId, version);
        if (pomXml == null) {
            throw new UnresolvableModelException("Has not been previously resolved", groupId, artifactId, version);
        }
        return new ModelSource2() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(pomXml);
            }

            @Override
            public String getLocation() {
                return pomXml.getAbsolutePath();
            }

            @Override
            public ModelSource2 getRelatedSource(String relPath) {
                return null;
            }

            @Override
            public URI getLocationURI() {
                return null;
            }
        };
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}
