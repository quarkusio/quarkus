package io.quarkus.keycloak.pep.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.keycloak")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface KeycloakPolicyEnforcerConfig {

    /**
     * The default tenant.
     */
    @WithParentName
    KeycloakPolicyEnforcerTenantConfig defaultTenant();

    /**
     * Additional named tenants.
     */
    @ConfigDocSection
    @ConfigDocMapKey("tenant")
    @WithParentName
    Map<String, KeycloakPolicyEnforcerTenantConfig> namedTenants();
}
