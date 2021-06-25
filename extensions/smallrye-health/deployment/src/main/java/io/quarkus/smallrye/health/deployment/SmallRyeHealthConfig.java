package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-health")
public class SmallRyeHealthConfig {

    /**
     * Root path for health-checking endpoints.
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     */
    @ConfigItem(defaultValue = "health")
    String rootPath;

    /**
     * The relative path of the liveness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @ConfigItem(defaultValue = "live")
    String livenessPath;

    /**
     * The relative path of the readiness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @ConfigItem(defaultValue = "ready")
    String readinessPath;

    /**
     * The relative path of the health group endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @ConfigItem(defaultValue = "group")
    String groupPath;

    /**
     * The relative path of the wellness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @ConfigItem(defaultValue = "well")
    String wellnessPath;

    /**
     * The relative path of the startup health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @ConfigItem(defaultValue = "started")
    String startupPath;

    /**
     * Whether the context should be propagated to each health check invocation.
     */
    @ConfigItem(defaultValue = "false")
    boolean contextPropagation;

    /**
     * SmallRye Health UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    SmallRyeHealthUIConfig ui;
}
