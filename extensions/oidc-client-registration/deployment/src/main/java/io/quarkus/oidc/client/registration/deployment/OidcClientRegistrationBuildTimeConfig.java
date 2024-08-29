package io.quarkus.oidc.client.registration.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC client registration.
 */
@ConfigRoot
public class OidcClientRegistrationBuildTimeConfig {
    /**
     * If the OIDC client registration extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
