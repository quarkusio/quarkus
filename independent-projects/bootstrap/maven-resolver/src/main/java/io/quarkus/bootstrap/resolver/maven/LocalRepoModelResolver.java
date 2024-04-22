package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

class LocalRepoModelResolver implements ModelResolver, LocalPomResolver {

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
