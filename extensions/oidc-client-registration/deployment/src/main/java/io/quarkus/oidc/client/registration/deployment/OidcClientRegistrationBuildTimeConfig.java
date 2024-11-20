package io.quarkus.oidc.client.registration.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for OIDC client registration.
 */
@ConfigMapping(prefix = "quarkus.oidc-client-registration")
@ConfigRoot
public interface OidcClientRegistrationBuildTimeConfig {
    /**
     * If the OIDC client registration extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
