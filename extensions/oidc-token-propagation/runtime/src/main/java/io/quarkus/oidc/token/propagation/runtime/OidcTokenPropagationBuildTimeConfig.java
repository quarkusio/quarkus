package io.quarkus.oidc.token.propagation.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC Token Propagation.
 */
@ConfigRoot
public class OidcTokenPropagationBuildTimeConfig {
    /**
     * If the OIDC Token Propagation is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
