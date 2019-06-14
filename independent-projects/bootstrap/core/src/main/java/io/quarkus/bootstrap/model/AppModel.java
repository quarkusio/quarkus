package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppModel {

    private final AppArtifact appArtifact;
    private final List<AppDependency> deploymentDeps;
    private final List<AppDependency> userDeps;
    private List<AppDependency> allDeps;

    public AppModel(AppArtifact appArtifact, List<AppDependency> userDeps, List<AppDependency> deploymentDeps) {
        this.appArtifact = appArtifact;
        this.userDeps = userDeps;
        this.deploymentDeps = deploymentDeps;
    }

    public List<AppDependency> getAllDependencies() throws BootstrapDependencyProcessingException {
        if(allDeps == null) {
            allDeps = new ArrayList<>(userDeps.size() + deploymentDeps.size());
            allDeps.addAll(userDeps);
            allDeps.addAll(deploymentDeps);
        }
        return allDeps;
    }

    public AppArtifact getAppArtifact() {
        return appArtifact;
    }

    public List<AppDependency> getUserDependencies() throws BootstrapDependencyProcessingException {
            return userDeps;
    }

    public List<AppDependency> getDeploymentDependencies() throws BootstrapDependencyProcessingException {
        return deploymentDeps;
    }
}
