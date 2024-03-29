package io.quarkus.oidc.deployment;

import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC.
 */
@ConfigRoot
public class OidcBuildTimeConfig {
    /**
     * If the OIDC extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Dev UI configuration.
     */
    @ConfigItem
    public DevUiConfig devui;
    /**
     * Enable the registration of the Default TokenIntrospection and UserInfo Cache implementation bean.
     * Note: This only enables the default implementation. It requires configuration to be activated.
     * See {@link OidcConfig#tokenCache}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean defaultTokenCacheEnabled;
}
