package io.quarkus.smallrye.health.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeHealthBuildItem extends SimpleBuildItem {

    private final String healthUiFinalDestination;
    private final String healthUiPath;

    public SmallRyeHealthBuildItem(String healthUiFinalDestination, String healthUiPath) {
        this.healthUiFinalDestination = healthUiFinalDestination;
        this.healthUiPath = healthUiPath;
    }

    public String getHealthUiFinalDestination() {
        return healthUiFinalDestination;
    }

    public String getHealthUiPath() {
        return healthUiPath;
    }
}