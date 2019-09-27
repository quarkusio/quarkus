package io.quarkus.resteasy.server.common.deployment;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResteasyDeploymentBuildItem extends SimpleBuildItem {
    private ResteasyDeployment deployment;
    private String rootPath;

    public ResteasyDeploymentBuildItem(String rootPath, ResteasyDeployment deployment) {
        this.deployment = deployment;
        this.rootPath = rootPath;
    }

    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    public String getRootPath() {
        return rootPath;
    }
}
