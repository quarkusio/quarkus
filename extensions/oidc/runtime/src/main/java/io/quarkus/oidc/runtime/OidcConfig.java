package io.quarkus.oidc.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc", phase = ConfigPhase.RUN_TIME)
public class OidcConfig {

    /**
     * The default tenant.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public OidcTenantConfig defaultTenant;

    /**
     * Additional named tenants.
     */
    @ConfigDocSection
    @ConfigDocMapKey("tenant")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, OidcTenantConfig> namedTenants;
}
