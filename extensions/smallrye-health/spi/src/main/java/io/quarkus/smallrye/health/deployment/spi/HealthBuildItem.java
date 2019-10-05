package io.quarkus.smallrye.health.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class HealthBuildItem extends MultiBuildItem {
    private String healthCheckClass;
    private boolean enabled;
    private String configRootName;

    /**
     * @param healthCheckClass the name of the health check class, needs to implements
     *        org.eclipse.microprofile.health.HealthCheck
     * @param enabled whether or not the check is enabled
     * @param configRootName the name of the root configuration of the extension as defined by the <code>@ConfigRoot</code>
     *        annotation.
     */
    public HealthBuildItem(String healthCheckClass, boolean enabled, String configRootName) {
        this.healthCheckClass = healthCheckClass;
        this.enabled = enabled;
        this.configRootName = configRootName;
    }

    public String getHealthCheckClass() {
        return this.healthCheckClass;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConfigRootName() {
        return configRootName;
    }
}
