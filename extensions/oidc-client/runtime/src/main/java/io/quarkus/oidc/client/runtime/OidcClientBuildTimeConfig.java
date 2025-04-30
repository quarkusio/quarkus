package io.quarkus.oidc.client.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for OIDC client.
 */
@ConfigMapping(prefix = "quarkus.oidc-client")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcClientBuildTimeConfig {
    /**
     * If the OIDC client extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
