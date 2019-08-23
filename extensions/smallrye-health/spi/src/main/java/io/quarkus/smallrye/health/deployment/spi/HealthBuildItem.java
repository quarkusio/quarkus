package io.quarkus.smallrye.health.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class HealthBuildItem extends MultiBuildItem {
    private Class<?> healthCheckClass;

    public HealthBuildItem(Class<?> healthCheckClass) {
        this.healthCheckClass = healthCheckClass;
    }

    public Class<?> getHealthCheckClass() {
        return this.healthCheckClass;
    }
}
