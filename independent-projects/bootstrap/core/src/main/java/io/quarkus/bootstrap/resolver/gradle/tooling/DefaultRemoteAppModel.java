package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppModel;

public class DefaultRemoteAppModel implements RemoteAppModel {

    private RemoteAppArtifact appArtifact;
    private List<RemoteAppDependency> userDependencies;
    private List<RemoteAppDependency> deploymentDependencies;

    public DefaultRemoteAppModel(RemoteAppArtifact artifact, List<RemoteAppDependency> userDependencies,
            List<RemoteAppDependency> deploymentDependencies) {
        this.appArtifact = artifact;
        this.userDependencies = userDependencies;
        this.deploymentDependencies = deploymentDependencies;
    }

    public static RemoteAppModel from(AppModel appModel) throws BootstrapDependencyProcessingException {
        List<RemoteAppDependency> userDependencies = appModel.getUserDependencies()
                .stream()
                .map(DefaultRemoteAppDependency::from)
                .collect(Collectors.toList());

        List<RemoteAppDependency> deploymentDependencies = appModel.getDeploymentDependencies()
                .stream()
                .map(DefaultRemoteAppDependency::from)
                .collect(Collectors.toList());

        RemoteAppArtifact artifact = DefaultRemoteAppArtifact.from(appModel.getAppArtifact());

        return new DefaultRemoteAppModel(artifact, userDependencies, deploymentDependencies);
    }

    @Override
    public RemoteAppArtifact getAppArtifact() {
        return appArtifact;
    }

    @Override
    public List<RemoteAppDependency> getUserDependencies() {
        return userDependencies;
    }

    @Override
    public List<RemoteAppDependency> getDeploymentDependencies() {
        return deploymentDependencies;
    }

}
