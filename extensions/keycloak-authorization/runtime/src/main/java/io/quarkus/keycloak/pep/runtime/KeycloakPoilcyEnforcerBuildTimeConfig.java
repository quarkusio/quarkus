package io.quarkus.keycloak.pep.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for Keycloak Authorization.
 */
@ConfigRoot(name = "keycloak")
public class KeycloakPoilcyEnforcerBuildTimeConfig {

    /**
     * Policy enforcement enable status
     */
    @ConfigItem
    public KeycloakPolicyEnforcerEnableStatus policyEnforcer = new KeycloakPolicyEnforcerEnableStatus();

    @ConfigGroup
    public static class KeycloakPolicyEnforcerEnableStatus {

        /**
         * Enables policy enforcement.
         */
        @ConfigItem
        public boolean enable;
    }
}
