package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "health")
public class HealthBuildTimeConfig {
    /**
     * Whether or not extensions published health check should be enabled.
     */
    @ConfigItem(name = "extensions.enabled", defaultValue = "true")
    public boolean extensionsEnabled;
}
