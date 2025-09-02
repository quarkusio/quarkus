package io.quarkus.oidc.deployment;

import io.quarkus.oidc.runtime.OidcConfig;
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
}
