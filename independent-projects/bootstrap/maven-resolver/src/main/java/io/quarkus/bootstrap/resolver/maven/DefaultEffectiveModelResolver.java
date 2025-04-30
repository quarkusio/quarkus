package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.building.*;
import org.apache.maven.model.resolution.ModelResolver;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;

class DefaultEffectiveModelResolver implements EffectiveModelResolver {

    private final MavenArtifactResolver resolver;
    private final ModelBuilder modelBuilder;
    private final ModelCache modelCache;
    private final Map<ArtifactCoords, Model> effectiveModels = new HashMap<>();

    DefaultEffectiveModelResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        try {
            modelCache = new BootstrapModelCache(resolver.getMavenContext().getRepositorySystemSession());
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven model resolver", e);
        }
        modelBuilder = BootstrapModelBuilderFactory.getDefaultModelBuilder();
    }

    public Model resolveEffectiveModel(ArtifactCoords coords) {
        return resolveEffectiveModel(coords, List.of());
    }

    public Model resolveEffectiveModel(ArtifactCoords coords, List<RemoteRepository> repos) {

        if (!ArtifactCoords.TYPE_POM.equals(coords.getType())) {
            coords = ArtifactCoords.pom(coords.getGroupId(), coords.getArtifactId(), coords.getVersion());
        }

        var cached = effectiveModels.get(coords);
        if (cached != null) {
            return cached;
        }

        final LocalWorkspace ws = resolver.getMavenContext().getWorkspace();
        if (ws != null) {
            final LocalProject project = ws.getProject(coords.getGroupId(), coords.getArtifactId());
            if (project != null && coords.getVersion().equals(project.getVersion())
                    && project.getEffectiveModel() != null) {
                return project.getEffectiveModel();
            }
        }

        final File pomFile;
        final ArtifactResult pomResult;
        try {
            pomResult = resolver.resolve(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                    coords.getClassifier(), coords.getType(), coords.getVersion()), repos);
            pomFile = pomResult.getArtifact().getFile();
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to resolve " + coords.toCompactCoords(), e);
        }

        final Model rawModel;
        try {
            rawModel = ModelUtils.readModel(pomFile.toPath());
        } catch (IOException e1) {
            throw new RuntimeException("Failed to read " + pomFile, e1);
        }

        final ModelResolver modelResolver;
        try {
            modelResolver = BootstrapModelResolver.newInstance(resolver.getMavenContext(), null);
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize model resolver", e);
        }

        // override the relative path to the parent in case it's in the local Maven repo
        Parent parent = rawModel.getParent();
        if (parent != null) {
            final Artifact parentPom = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(),
                    ArtifactCoords.TYPE_POM, parent.getVersion());
            final ArtifactResult parentResult;
            final Path parentPomPath;
            try {
                parentResult = resolver.resolve(parentPom, repos);
                parentPomPath = parentResult.getArtifact().getFile().toPath();
            } catch (BootstrapMavenException e) {
                throw new RuntimeException("Failed to resolve " + parentPom, e);
            }
            rawModel.getParent().setRelativePath(pomFile.toPath().getParent().relativize(parentPomPath).toString());

            String repoUrl = null;
            for (RemoteRepository r : repos) {
                if (r.getId().equals(parentResult.getRepository().getId())) {
                    repoUrl = r.getUrl();
                    break;
                }
            }
            if (repoUrl != null) {
                Repository modelRepo = null;
                for (Repository r : rawModel.getRepositories()) {
                    if (r.getId().equals(parentResult.getRepository().getId())) {
                        modelRepo = r;
                        break;
                    }
                }
                if (modelRepo == null) {
                    modelRepo = new Repository();
                    modelRepo.setId(parentResult.getRepository().getId());
                    modelRepo.setLayout("default");
                    modelRepo.setReleases(new RepositoryPolicy());
                }
                modelRepo.setUrl(repoUrl);

                try {
                    modelResolver.addRepository(modelRepo, false);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to add repository " + modelRepo, e);
                }
            }
        }

        final ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setPomFile(pomFile);
        req.setRawModel(rawModel);
        req.setModelResolver(modelResolver);
        req.setSystemProperties(System.getProperties());
        req.setUserProperties(System.getProperties());
        req.setModelCache(modelCache);

        try {
            return modelBuilder.build(req).getEffectiveModel();
        } catch (ModelBuildingException e) {
            throw new RuntimeException("Failed to resolve the effective model of " + coords.toCompactCoords(), e);
        }
    }

    static class BootstrapModelCache implements ModelCache {

        private final RepositorySystemSession session;

        private final RepositoryCache cache;

        BootstrapModelCache(RepositorySystemSession session) {
            this.session = session;
            this.cache = session.getCache() == null ? new DefaultRepositoryCache() : session.getCache();
        }

        @Override
        public Object get(String groupId, String artifactId, String version, String tag) {
            return cache.get(session, new Key(groupId, artifactId, version, tag));
        }

        @Override
        public void put(String groupId, String artifactId, String version, String tag, Object data) {
            cache.put(session, new Key(groupId, artifactId, version, tag), data);
        }

        static class Key {

            private final String groupId;
            private final String artifactId;
            private final String version;
            private final String tag;
            private final int hash;

            public Key(String groupId, String artifactId, String version, String tag) {
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.tag = tag;

                int h = 17;
                h = h * 31 + this.groupId.hashCode();
                h = h * 31 + this.artifactId.hashCode();
                h = h * 31 + this.version.hashCode();
                h = h * 31 + this.tag.hashCode();
                hash = h;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (null == obj || !getClass().equals(obj.getClass())) {
                    return false;
                }

                Key that = (Key) obj;
                return artifactId.equals(that.artifactId) && groupId.equals(that.groupId)
                        && version.equals(that.version) && tag.equals(that.tag);
            }

            @Override
            public int hashCode() {
                return hash;
            }
        }
    }
}
