package io.quarkus.narayana.lra.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * LRA build time configuration properties
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public final class LRABuildTimeConfiguration {

    /**
     * Whether to include LRA proxy endpoints in the generated OpenAPI document
     */
    @ConfigItem(name = "openapi.included", defaultValue = "false")
    public boolean openapiIncluded;
}
