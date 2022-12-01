package io.quarkus.oidc.token.propagation.reactive;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC Token Propagation Reactive.
 */
@ConfigRoot
public class OidcTokenPropagationReactiveBuildTimeConfig {
    /**
     * If the OIDC Token Reactive Propagation is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
