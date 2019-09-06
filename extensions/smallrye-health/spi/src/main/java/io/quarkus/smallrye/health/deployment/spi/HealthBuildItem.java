package io.quarkus.smallrye.health.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class HealthBuildItem extends MultiBuildItem {
    private Class<?> healthCheckClass;
    private boolean enabled;

    public HealthBuildItem(Class<?> healthCheckClass, boolean enabled) {
        this.healthCheckClass = healthCheckClass;
        this.enabled = enabled;
    }

    public Class<?> getHealthCheckClass() {
        return this.healthCheckClass;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
