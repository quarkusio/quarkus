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
     * Note it only allows to use the default implementation, one needs to configure it in order to activate it,
     * please see {@link OidcConfig#tokenCache}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean defaultTokenCacheEnabled;

    /**
     * Whether to include the OIDC application endpoints in the generated OpenAPI document.
     * For example, OIDC may expose logout endpoints that are not included by default.
     * If you want the OpenAPI document to include paths defined by runtime configuration property, as are
     * `quarkus.oidc.logout.path`, `quarkus.oidc.logout.backchannel.path` and `quarkus.oidc.logout.frontchannel.path`,
     * you must define them in the Quarkus configuration file at build time.
     */
    @ConfigItem(defaultValue = "false", name = "openapi.included")
    public boolean openapiIncluded;
}
