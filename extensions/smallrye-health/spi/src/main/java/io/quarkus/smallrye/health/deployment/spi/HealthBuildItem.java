package io.quarkus.smallrye.health.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class HealthBuildItem extends MultiBuildItem {

    private final String healthCheckClass;

    private final boolean enabled;

    /**
     * 
     * @param healthCheckClass
     * @param enabled
     * @param configRootName This parameter is not used
     * @deprecated Use {@link #HealthBuildItem(String, boolean)} instead.
     */
    @Deprecated
    public HealthBuildItem(String healthCheckClass, boolean enabled, String configRootName) {
        this(healthCheckClass, enabled);
    }

    /**
     * @param healthCheckClass the name of the health check class, needs to implements
     *        org.eclipse.microprofile.health.HealthCheck
     * @param enabled whether or not the check is enabled
     */
    public HealthBuildItem(String healthCheckClass, boolean enabled) {
        this.healthCheckClass = healthCheckClass;
        this.enabled = enabled;
    }

    public String getHealthCheckClass() {
        return this.healthCheckClass;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
