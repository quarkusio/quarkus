package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;

public class TenantConfigBean {

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final Function<OidcTenantConfig, Uni<TenantConfigContext>> tenantConfigContextFactory;

    public TenantConfigBean(
            Map<String, TenantConfigContext> staticTenantsConfig,
            Map<String, TenantConfigContext> dynamicTenantsConfig,
            TenantConfigContext defaultTenant,
            Function<OidcTenantConfig, Uni<TenantConfigContext>> tenantConfigContextFactory,
            Executor blockingExecutor) {
        this.staticTenantsConfig = staticTenantsConfig;
        this.dynamicTenantsConfig = dynamicTenantsConfig;
        this.defaultTenant = defaultTenant;
        this.tenantConfigContextFactory = tenantConfigContextFactory;
    }

    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return staticTenantsConfig;
    }

    public TenantConfigContext getDefaultTenant() {
        return defaultTenant;
    }

    public Function<OidcTenantConfig, Uni<TenantConfigContext>> getTenantConfigContextFactory() {
        return tenantConfigContextFactory;
    }

    public Map<String, TenantConfigContext> getDynamicTenantsConfig() {
        return dynamicTenantsConfig;
    }
}
