package io.quarkus.narayana.lra.deployment;

import io.quarkus.narayana.lra.deployment.devservice.LRACoordinatorDevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * LRA build time configuration properties
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.lra")
public interface LRABuildTimeConfiguration {

    /**
     * Whether to include LRA proxy endpoints in the generated OpenAPI document
     */
    @WithName("openapi.included")
    @WithDefault("false")
    boolean openapiIncluded();

    /**
     * Dev Services.
     * <p>
     * Dev Services that automatically start Narayana LRA coordinator in dev and test modes.
     */
    @ConfigDocSection(generated = true)
    LRACoordinatorDevServicesBuildTimeConfig devservices();
}
