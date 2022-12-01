package io.quarkus.keycloak.pep.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "keycloak", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class KeycloakPolicyEnforcerConfig {

    /**
     * The default tenant.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public KeycloakPolicyEnforcerTenantConfig defaultTenant;

    /**
     * Additional named tenants.
     */
    @ConfigDocSection
    @ConfigDocMapKey("tenant")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, KeycloakPolicyEnforcerTenantConfig> namedTenants;
}
