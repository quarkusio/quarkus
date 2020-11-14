package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-health")
public class SmallRyeHealthConfig {

    /**
     * Root path for health-checking endpoints.
     */
    @ConfigItem(defaultValue = "/health")
    String rootPath;

    /**
     * The relative path of the liveness health-checking endpoint.
     */
    @ConfigItem(defaultValue = "/live")
    String livenessPath;

    /**
     * The relative path of the readiness health-checking endpoint.
     */
    @ConfigItem(defaultValue = "/ready")
    String readinessPath;

    /**
     * The relative path of the health group endpoint.
     */
    @ConfigItem(defaultValue = "/group")
    String groupPath;

    /**
     * The relative path of the wellness health-checking endpoint.
     */
    @ConfigItem(defaultValue = "/well")
    String wellnessPath;

    /**
     * SmallRye Health UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    SmallRyeHealthUIConfig ui;
}
