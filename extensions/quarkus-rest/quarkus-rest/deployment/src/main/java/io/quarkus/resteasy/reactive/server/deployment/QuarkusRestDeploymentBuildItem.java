package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class QuarkusRestDeploymentBuildItem extends SimpleBuildItem {

    private final RuntimeValue<QuarkusRestDeployment> deployment;
    private final String applicationPath;

    public QuarkusRestDeploymentBuildItem(RuntimeValue<QuarkusRestDeployment> deployment, String applicationPath) {
        this.deployment = deployment;
        this.applicationPath = applicationPath;
    }

    public RuntimeValue<QuarkusRestDeployment> getDeployment() {
        return deployment;
    }

    public String getApplicationPath() {
        return applicationPath;
    }
}
