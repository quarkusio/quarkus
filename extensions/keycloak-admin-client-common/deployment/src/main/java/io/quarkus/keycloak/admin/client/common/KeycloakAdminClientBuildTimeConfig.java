package io.quarkus.keycloak.admin.client.common;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Keycloak Admin Client
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME, name = "keycloak.admin-client")
public class KeycloakAdminClientBuildTimeConfig {

    /**
     * Set to true if Keycloak Admin Client injection is supported.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;

}
