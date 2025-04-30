package io.quarkus.smallrye.health.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.smallrye-health")
public interface SmallRyeHealthBuildTimeConfig {
    /**
     * Activate or disable this extension. Disabling this extension means that no health related information is exposed.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether extensions published health check should be enabled.
     */
    @WithName("extensions.enabled")
    @WithDefault("true")
    boolean extensionsEnabled();

    /**
     * Whether to include the Liveness and Readiness Health endpoints in the generated OpenAPI document
     */
    @WithName("openapi.included")
    @WithDefault("false")
    boolean openapiIncluded();

    /**
     * Root path for health-checking endpoints.
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     * If the management interface is enabled, the value will be resolved as a path relative to
     * `${quarkus.management.root-path}`.
     */
    @WithDefault("health")
    String rootPath();

    /**
     * The relative path of the liveness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @WithDefault("live")
    String livenessPath();

    /**
     * The relative path of the readiness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @WithDefault("ready")
    String readinessPath();

    /**
     * The relative path of the health group endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @WithDefault("group")
    String groupPath();

    /**
     * The relative path of the wellness health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @WithDefault("well")
    String wellnessPath();

    /**
     * The relative path of the startup health-checking endpoint.
     * By default, this value will be resolved as a path relative to `${quarkus.smallrye-health.rootPath}`.
     */
    @WithDefault("started")
    String startupPath();

    /**
     * Whether the context should be propagated to each health check invocation.
     */
    @WithDefault("false")
    boolean contextPropagation();

    /**
     * The number of the maximum health groups that can be created.
     */
    OptionalInt maxGroupRegistriesCount();

    /**
     * The name of the default health group used when no other health group is defined on the health check.
     */
    Optional<String> defaultHealthGroup();

    /**
     * If management interface is turned on the health endpoints and ui will be published under the management interface. This
     * allows you to exclude Health from management by setting the value to false
     */
    @WithName("management.enabled")
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * SmallRye Health UI configuration
     */
    @ConfigDocSection
    SmallRyeHealthUIConfig ui();
}
