package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.tooling.ProjectConnection;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;

public class RemoteAppModelGradleResolver implements AppModelResolver {

    private final ProjectConnection connection;

    public RemoteAppModelGradleResolver(ProjectConnection connection) {
        this.connection = connection;
    }

    private static AppArtifact resolveRemoteArtifact(RemoteAppArtifact remote) {
        AppArtifact appArtifact = new AppArtifact(remote.getGroupId(), remote.getArtifactId(), remote.getClassifier(),
                remote.getType(), remote.getVersion());
        appArtifact.setPath(remote.getPath() == null ? null : Paths.get(remote.getPath()));

        return appArtifact;
    }

    private static AppDependency resolveRemoteDependency(RemoteAppDependency remote) {
        return new AppDependency(resolveRemoteArtifact(remote.getArtifact()), remote.getScope(), remote.isOptional());
    }

    @Override
    public void relink(AppArtifact appArtifact, Path localPath) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolve(AppArtifact artifact) throws AppModelResolverException {
        if (!artifact.isResolved()) {
            throw new AppModelResolverException("Artifact has not been resolved: " + artifact);
        }
        return artifact.getPath();
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact artifact, List<AppDependency> deps)
            throws AppModelResolverException {
        return Collections.emptyList();
    }

    @Override
    public AppModel resolveModel(AppArtifact artifact) throws AppModelResolverException {
        RemoteAppModel remoteModel = connection.getModel(RemoteAppModel.class);
        List<AppDependency> userDependencies = remoteModel.getUserDependencies()
                .stream()
                .map(RemoteAppModelGradleResolver::resolveRemoteDependency)
                .collect(Collectors.toList());

        List<AppDependency> deploymentDependencies = remoteModel.getDeploymentDependencies()
                .stream()
                .map(RemoteAppModelGradleResolver::resolveRemoteDependency)
                .collect(Collectors.toList());

        AppArtifact resolvedArtifact = resolveRemoteArtifact(remoteModel.getAppArtifact());
        artifact.setPath(resolvedArtifact.getPath());

        return new AppModel(artifact, userDependencies, deploymentDependencies);
    }

    @Override
    public AppModel resolveModel(AppArtifact root, List<AppDependency> deps) throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listLaterVersions(AppArtifact artifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextVersion(AppArtifact artifact, String fromVersion, boolean fromVersionIncluded,
            String upToVersion, boolean upToVersionIncluded) throws AppModelResolverException {
        throw new UnsupportedOperationException();

    }

    @Override
    public String getLatestVersion(AppArtifact artifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        throw new UnsupportedOperationException();
    }

}
