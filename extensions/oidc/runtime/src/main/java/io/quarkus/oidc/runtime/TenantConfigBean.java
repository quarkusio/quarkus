package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.function.Function;

import io.quarkus.oidc.OidcTenantConfig;

public class TenantConfigBean {

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory;

    public TenantConfigBean(Map<String, TenantConfigContext> staticTenantsConfig, TenantConfigContext defaultTenant,
            Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory) {
        this.staticTenantsConfig = staticTenantsConfig;
        this.defaultTenant = defaultTenant;
        this.tenantConfigContextFactory = tenantConfigContextFactory;
    }

    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return staticTenantsConfig;
    }

    public TenantConfigContext getDefaultTenant() {
        return defaultTenant;
    }

    public Function<OidcTenantConfig, TenantConfigContext> getTenantConfigContextFactory() {
        return tenantConfigContextFactory;
    }
}
