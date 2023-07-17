package io.quarkus.kubernetes.deployment;

import io.quarkus.kubernetes.spi.DeployStrategy;

public class DeploymentTargetEntry {
    private final String name;
    private final String kind;
    private final int priority;
    private final DeployStrategy deployStrategy;

    public DeploymentTargetEntry(String name, String kind, int priority, DeployStrategy deployStrategy) {
        this.name = name;
        this.kind = kind;
        this.priority = priority;
        this.deployStrategy = deployStrategy;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public int getPriority() {
        return priority;
    }

    public DeployStrategy getDeployStrategy() {
        return deployStrategy;
    }
}
