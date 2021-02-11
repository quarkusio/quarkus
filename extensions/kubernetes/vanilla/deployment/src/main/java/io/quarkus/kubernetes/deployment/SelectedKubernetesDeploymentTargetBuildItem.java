package io.quarkus.kubernetes.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SelectedKubernetesDeploymentTargetBuildItem extends SimpleBuildItem {

    private final DeploymentTargetEntry entry;

    public SelectedKubernetesDeploymentTargetBuildItem(DeploymentTargetEntry entry) {
        this.entry = entry;
    }

    public DeploymentTargetEntry getEntry() {
        return entry;
    }
}
