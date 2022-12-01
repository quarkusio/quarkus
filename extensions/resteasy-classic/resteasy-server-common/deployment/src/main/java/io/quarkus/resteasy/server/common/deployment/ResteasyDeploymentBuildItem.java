package io.quarkus.resteasy.server.common.deployment;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResteasyDeploymentBuildItem extends SimpleBuildItem {
    private String rootPath;
    private ResteasyDeployment deployment;

    public ResteasyDeploymentBuildItem(String rootPath, ResteasyDeployment deployment) {
        this.rootPath = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
        this.deployment = deployment;
    }

    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    public String getRootPath() {
        return rootPath;
    }
}
