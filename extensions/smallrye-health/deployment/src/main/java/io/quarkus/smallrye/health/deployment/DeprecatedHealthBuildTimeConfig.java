package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This class is deprecated, don't add any more properties here.
 *
 * When dropping this class please make the properties in {@link SmallRyeHealthBuildTimeConfig} non optional.
 *
 * @deprecated Use {@link SmallRyeHealthBuildTimeConfig} instead.
 */
@ConfigRoot(name = "health")
@Deprecated(since = "3.14", forRemoval = true)
public class DeprecatedHealthBuildTimeConfig {

    /**
     * Whether extensions published health check should be enabled.
     *
     * @deprecated Use {@code quarkus.smallrye-health.extensions.enabled} instead.
     */
    @ConfigItem(name = "extensions.enabled", defaultValue = "true")
    @Deprecated(since = "3.14", forRemoval = true)
    public boolean extensionsEnabled;

    /**
     * Whether to include the Liveness and Readiness Health endpoints in the generated OpenAPI document
     *
     * @deprecated Use {@code quarkus.smallrye-health.openapi.included} instead.
     */
    @ConfigItem(name = "openapi.included", defaultValue = "false")
    @Deprecated(since = "3.14", forRemoval = true)
    public boolean openapiIncluded;
}
