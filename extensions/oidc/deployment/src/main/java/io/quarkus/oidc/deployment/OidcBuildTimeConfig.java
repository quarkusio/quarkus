package io.quarkus.oidc.deployment;

import java.util.Set;

import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Build time configuration for OIDC.
 */
@ConfigMapping(prefix = "quarkus.oidc")
@ConfigRoot
public interface OidcBuildTimeConfig {
    /**
     * If the OIDC extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * OIDC Dev UI configuration which is effective in dev mode only.
     */
    @ConfigDocSection
    DevUiConfig devui();

    /**
     * Enable the registration of the Default TokenIntrospection and UserInfo Cache implementation bean.
     * Note: This only enables the default implementation. It requires configuration to be activated.
     * See {@link OidcConfig#tokenCache}.
     */
    @WithDefault("true")
    boolean defaultTokenCacheEnabled();

    /**
     * Whether the OIDC extension should automatically register a health check for OIDC tenants
     * when a Health Check capability is present.
     */
    @WithName("health.enabled")
    @WithDefault("false")
    boolean healthEnabled();

    /**
     * OIDC routes that tenants can use. Each route requires a dedicated HTTP handler;
     * remove routes no tenant needs to avoid unnecessary overhead.
     */
    @WithDefault("backchannel-logout,resource-metadata")
    Set<OidcRoute> allowedRoutes();

    /**
     * OIDC routes that tenants can use when enabled via {@link #allowedRoutes()}.
     */
    enum OidcRoute {
        /**
         * Allows tenants to use back-channel logout by accepting logout notifications from the OIDC provider.
         *
         * @see OidcTenantConfig.Backchannel
         */
        BACKCHANNEL_LOGOUT,
        /**
         * Allows tenants to publish protected resource metadata as defined by
         * <a href="https://datatracker.ietf.org/doc/rfc9728/">RFC 9728</a>.
         *
         * @see OidcTenantConfig.ResourceMetadata
         */
        RESOURCE_METADATA
    }
}
