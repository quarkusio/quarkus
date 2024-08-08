package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "health")
public class HealthBuildTimeConfig {
    /**
     * Activate or disable this extension. Disabling this extension means that no health related information is exposed.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Whether extensions published health check should be enabled.
     */
    @ConfigItem(name = "extensions.enabled", defaultValue = "true")
    public boolean extensionsEnabled;

    /**
     * Whether to include the Liveness and Readiness Health endpoints in the generated OpenAPI document
     */
    @ConfigItem(name = "openapi.included", defaultValue = "false")
    public boolean openapiIncluded;
}
