package io.quarkus.oidc.deployment.devservices.keycloak;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC.
 */
@ConfigRoot
public class KeycloakBuildTimeConfig {
    /**
     * Dev Services.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public DevServicesConfig devservices;
}
