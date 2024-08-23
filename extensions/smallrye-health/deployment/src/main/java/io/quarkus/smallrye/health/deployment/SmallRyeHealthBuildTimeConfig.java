package io.quarkus.smallrye.health.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-health")
public class SmallRyeHealthBuildTimeConfig {
    /**
     * Activate or disable this extension. Disabling this extension means that no health related information is exposed.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Whether extensions published health check should be enabled.
     */
    @ConfigItem(name = "extensions.enabled", defaultValueDocumentation = "true")
    public Optional<Boolean> extensionsEnabled;

    /**
     * Whether to include the Liveness and Readiness Health endpoints in the generated OpenAPI document
     */
    @ConfigItem(name = "openapi.included", defaultValueDocumentation = "false")
    public Optional<Boolean> openapiIncluded;
}
