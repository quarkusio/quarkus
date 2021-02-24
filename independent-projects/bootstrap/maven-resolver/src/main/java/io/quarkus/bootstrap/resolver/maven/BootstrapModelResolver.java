package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

public class BootstrapModelResolver implements ModelResolver {

    public static ModelResolver newInstance(BootstrapMavenContext ctx, LocalWorkspace workspace)
            throws BootstrapMavenException {
        final RepositorySystem repoSystem = ctx.getRepositorySystem();
        return new BootstrapModelResolver(
                new DefaultRepositorySystemSession(ctx.getRepositorySystemSession()).setWorkspaceReader(workspace), null, null,
                new ArtifactResolver() {
                    @Override
                    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request)
                            throws ArtifactResolutionException {
                        return repoSystem.resolveArtifact(session, request);
                    }

                    @Override
                    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session,
                            Collection<? extends ArtifactRequest> requests) throws ArtifactResolutionException {
                        return repoSystem.resolveArtifacts(session, requests);
                    }
                }, new VersionRangeResolver() {
                    @Override
                    public VersionRangeResult resolveVersionRange(RepositorySystemSession session,
                            VersionRangeRequest request) throws VersionRangeResolutionException {
                        return repoSystem.resolveVersionRange(session, request);
                    }
                }, ctx.getRemoteRepositoryManager(), ctx.getRemoteRepositories());
    }

    private final RepositorySystemSession session;
    private final RequestTrace trace;
    private final String context;
    private List<RemoteRepository> repositories;
    private final List<RemoteRepository> externalRepositories;
    private final ArtifactResolver resolver;
    private final VersionRangeResolver versionRangeResolver;
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final Set<String> repositoryIds;

    BootstrapModelResolver(RepositorySystemSession session, RequestTrace trace, String context,
            ArtifactResolver resolver, VersionRangeResolver versionRangeResolver,
            RemoteRepositoryManager remoteRepositoryManager, List<RemoteRepository> repositories) {
        this.session = session;
        this.trace = trace;
        this.context = context;
        this.resolver = resolver;
        this.versionRangeResolver = versionRangeResolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.repositories = repositories;
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));
        this.repositoryIds = new HashSet<>();
    }

    private BootstrapModelResolver(BootstrapModelResolver original) {
        this.session = original.session;
        this.trace = original.trace;
        this.context = original.context;
        this.resolver = original.resolver;
        this.versionRangeResolver = original.versionRangeResolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.repositories = new ArrayList<>(original.repositories);
        this.externalRepositories = original.externalRepositories;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
    }

    @Override
    public void addRepository(Repository repository)
            throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace)
            throws InvalidRepositoryException {
        if (session.isIgnoreArtifactDescriptorRepositories()) {
            return;
        }

        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            removeMatchingRepository(repositories, repository.getId());
        }

        List<RemoteRepository> newRepositories = Collections
                .singletonList(ArtifactDescriptorUtils.toRemoteRepository(repository));

        this.repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator<RemoteRepository> iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository remoteRepository = iterator.next();
            if (remoteRepository.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @Override
    public ModelResolver newCopy() {
        return new BootstrapModelResolver(this);
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
            request.setTrace(trace);
            pomArtifact = resolver.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        File pomFile = pomArtifact.getFile();

        return new FileModelSource(pomFile);
    }

    @Override
    public ModelSource resolveModel(final Parent parent)
            throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom",
                    parent.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = versionRangeResolver.resolveVersionRange(session,
                    versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format("No versions matched the requested parent version range '%s'",
                                parent.getVersion()),
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new UnresolvableModelException(
                        String.format("The requested parent version range '%s' does not specify an upper bound",
                                parent.getVersion()),
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            }

            parent.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        } catch (final VersionRangeResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), parent.getGroupId(), parent.getArtifactId(),
                    parent.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency)
            throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), "",
                    "pom", dependency.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = versionRangeResolver.resolveVersionRange(session,
                    versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format("No versions matched the requested dependency version range '%s'",
                                dependency.getVersion()),
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-4463 core IT.
                throw new UnresolvableModelException(
                        String.format("The requested dependency version range '%s' does not specify an upper bound",
                                dependency.getVersion()),
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            }

            dependency.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getVersion(), e);
        }
    }
}
