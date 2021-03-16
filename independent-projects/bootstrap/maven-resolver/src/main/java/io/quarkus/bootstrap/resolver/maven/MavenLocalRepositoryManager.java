package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.util.IoUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenLocalRepositoryManager implements LocalRepositoryManager {

    private final LocalRepositoryManager delegate;
    private final Path secondaryRepo;
    private final Path originalRepo;

    public MavenLocalRepositoryManager(LocalRepositoryManager delegate, Path secondaryRepo) {
        this.delegate = delegate;
        this.secondaryRepo = secondaryRepo;
        this.originalRepo = delegate.getRepository().getBasedir().toPath();
    }

    @Override
    public LocalRepository getRepository() {
        return delegate.getRepository();
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        return delegate.getPathForLocalArtifact(artifact);
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteArtifact(artifact, repository, context);
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }

    public void relink(String groupId, String artifactId, String classifier, String type, String version, Path p) {
        final Path creatorRepoPath = getLocalPath(originalRepo, groupId, artifactId, classifier, type, version);
        try {
            IoUtils.copy(p, creatorRepoPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy " + p + " to a staging repo", e);
        }
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        final LocalArtifactResult result = delegate.find(session, request);
        if (result.isAvailable()) {
            return result;
        }
        final Artifact artifact = request.getArtifact();
        final Path secondaryLocation = getLocalPath(secondaryRepo, artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        if (!Files.exists(secondaryLocation)) {
            return result;
        }
        result.setFile(secondaryLocation.toFile());
        artifact.setFile(result.getFile());
        result.setAvailable(true);
        return result;
    }

    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        delegate.add(session, request);
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        final LocalMetadataResult result = delegate.find(session, request);
        if (result.getFile() != null && result.getFile().exists()) {
            return result;
        }
        final Metadata metadata = request.getMetadata();
        final Path userRepoPath = getMetadataPath(secondaryRepo, metadata.getGroupId(), metadata.getArtifactId(),
                metadata.getType(), metadata.getVersion());
        if (!Files.exists(userRepoPath)) {
            return result;
        }
        result.setFile(userRepoPath.toFile());
        metadata.setFile(result.getFile());
        return result;
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        delegate.add(session, request);
    }

    private Path getMetadataPath(Path repoHome, String groupId, String artifactId, String type, String version) {
        Path p = repoHome;
        final String[] groupParts = groupId.split("\\.");
        for (String part : groupParts) {
            p = p.resolve(part);
        }
        if (artifactId != null) {
            p = p.resolve(artifactId);
        }
        if (version != null) {
            p = p.resolve(version);
        }
        return p.resolve("maven-metadata-local.xml");
    }

    private Path getLocalPath(Path repoHome, String groupId, String artifactId, String classifier, String type,
            String version) {
        Path p = repoHome;
        final String[] groupParts = groupId.split("\\.");
        for (String part : groupParts) {
            p = p.resolve(part);
        }
        final StringBuilder fileName = new StringBuilder();
        fileName.append(artifactId).append('-').append(version);
        if (classifier != null && !classifier.isEmpty()) {
            fileName.append('-').append(classifier);
        }
        fileName.append('.').append(type);
        return p.resolve(artifactId).resolve(version).resolve(fileName.toString());
    }
}
