package io.quarkus.oidc.client.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC client.
 */
@ConfigRoot(name = "oidc-client", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcClientBuildTimeConfig {
    /**
     * If the OIDC client extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
