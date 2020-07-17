package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class EnabledKubernetesDeploymentTargetsBuildItem extends SimpleBuildItem {

    private final List<DeploymentTargetEntry> entriesSortedByPriority;

    public EnabledKubernetesDeploymentTargetsBuildItem(List<DeploymentTargetEntry> entriesSortedByPriority) {
        if (entriesSortedByPriority.isEmpty()) {
            throw new IllegalArgumentException("At least one enabled entry must be active");
        }
        this.entriesSortedByPriority = entriesSortedByPriority;
    }

    public List<DeploymentTargetEntry> getEntriesSortedByPriority() {
        return entriesSortedByPriority;
    }

}
