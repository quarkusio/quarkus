package io.quarkus.oidc.deployment.devservices.keycloak;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC.
 */
@ConfigRoot
public class KeycloakBuildTimeConfig {
    /**
     * Dev services configuration.
     */
    @ConfigItem
    public DevServicesConfig devservices;
}
