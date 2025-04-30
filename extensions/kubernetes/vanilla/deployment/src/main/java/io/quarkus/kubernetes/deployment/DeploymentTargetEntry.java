package io.quarkus.kubernetes.deployment;

import io.quarkus.kubernetes.spi.DeployStrategy;

public class DeploymentTargetEntry {
    private final String name;
    private final DeploymentResourceKind deploymentResourceKind;
    private final int priority;
    private final DeployStrategy deployStrategy;

    public DeploymentTargetEntry(String name, DeploymentResourceKind kind, int priority, DeployStrategy deployStrategy) {
        this.name = name;
        this.deploymentResourceKind = kind;
        this.priority = priority;
        this.deployStrategy = deployStrategy;
    }

    public String getName() {
        return name;
    }

    public DeploymentResourceKind getDeploymentResourceKind() {
        return deploymentResourceKind;
    }

    public int getPriority() {
        return priority;
    }

    public DeployStrategy getDeployStrategy() {
        return deployStrategy;
    }
}
