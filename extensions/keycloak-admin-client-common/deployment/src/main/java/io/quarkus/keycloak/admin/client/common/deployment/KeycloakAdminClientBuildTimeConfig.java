package io.quarkus.keycloak.admin.client.common.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Keycloak Admin Client
 */
@ConfigMapping(prefix = "quarkus.keycloak.admin-client")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KeycloakAdminClientBuildTimeConfig {

    /**
     * Set to true if Keycloak Admin Client injection is supported.
     */
    @WithDefault("true")
    boolean enabled();

}
