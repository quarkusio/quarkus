package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.DeploymentInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResteasyReactiveDeploymentInfoBuildItem extends SimpleBuildItem {

    private final DeploymentInfo deploymentInfo;

    public ResteasyReactiveDeploymentInfoBuildItem(DeploymentInfo deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
    }

    public DeploymentInfo getDeploymentInfo() {
        return deploymentInfo;
    }
}
