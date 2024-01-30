package io.quarkus.oidc.proxy.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC Proxy.
 */
@ConfigRoot
public class OidcProxyBuildTimeConfig {
    /**
     * If the OIDC Proxy extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
