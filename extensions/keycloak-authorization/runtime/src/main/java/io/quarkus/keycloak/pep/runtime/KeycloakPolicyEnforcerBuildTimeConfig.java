package io.quarkus.keycloak.pep.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for Keycloak Authorization.
 */
@ConfigMapping(prefix = "quarkus.keycloak")
@ConfigRoot
public interface KeycloakPolicyEnforcerBuildTimeConfig {

    /**
     * Policy enforcement enable status
     */
    KeycloakPolicyEnforcerEnableStatus policyEnforcer();

    @ConfigGroup
    interface KeycloakPolicyEnforcerEnableStatus {

        /**
         * Enables policy enforcement.
         */
        @WithDefault("false")
        boolean enable();
    }
}
