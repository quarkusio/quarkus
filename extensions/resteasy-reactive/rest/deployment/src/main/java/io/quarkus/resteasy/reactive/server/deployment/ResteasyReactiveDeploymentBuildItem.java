package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.Deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class ResteasyReactiveDeploymentBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Deployment> deployment;
    private final String applicationPath;

    public ResteasyReactiveDeploymentBuildItem(RuntimeValue<Deployment> deployment, String applicationPath) {
        this.deployment = deployment;
        this.applicationPath = applicationPath;
    }

    public RuntimeValue<Deployment> getDeployment() {
        return deployment;
    }

    public String getApplicationPath() {
        return applicationPath;
    }
}
