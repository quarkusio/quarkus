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
     * Enable the registration of the Default TokenIntrospection and UserInfo Cache implementation bean.
     * Note it only allows to use the default implementation, one needs to configure it in order to activate it,
     * please see {@link OidcConfig#tokenCache}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean defaultTokenCacheEnabled;
}
