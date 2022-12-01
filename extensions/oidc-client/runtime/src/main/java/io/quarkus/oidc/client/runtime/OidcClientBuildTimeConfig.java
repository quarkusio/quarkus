package io.quarkus.oidc.client.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC client.
 */
@ConfigRoot
public class OidcClientBuildTimeConfig {
    /**
     * If the OIDC client extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
