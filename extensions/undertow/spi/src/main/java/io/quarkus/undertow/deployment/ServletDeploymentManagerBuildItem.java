package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.undertow.servlet.api.DeploymentManager;

public final class ServletDeploymentManagerBuildItem extends SimpleBuildItem {

    private final DeploymentManager deploymentManager;

    public ServletDeploymentManagerBuildItem(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }
}
