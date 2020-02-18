package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTenantConfigResolver {

    private static final String CURRENT_TENANT_CONFIG = "io.quarkus.oidc.current.tenant.config";

    @Inject
    Instance<TenantResolver> tenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    private volatile Map<String, TenantConfigContext> tenantsConfig;
    private volatile TenantConfigContext defaultTenant;
    private volatile Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory;

    @PostConstruct
    public void verifyResolvers() {
        if (tenantConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
        }
        if (tenantResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantResolver.class + " beans registered");
        }
    }

    /**
     * Resolve {@linkplain TenantConfigContext} which contains the tenant configuration and
     * the active OIDC connection instance which may be null.
     * 
     * @param context the current request context
     * @param create if true then the OIDC connection must be available or established
     *        for the resolution to be successful
     * @return
     */
    TenantConfigContext resolve(RoutingContext context, boolean create) {
        TenantConfigContext config = getTenantConfigFromConfigResolver(context, create);

        if (config == null) {
            config = getTenantConfigFromTenantResolver(context);
        }

        return config;
    }

    void setTenantsConfig(Map<String, TenantConfigContext> tenantsConfig) {
        this.tenantsConfig = tenantsConfig;
    }

    void setDefaultTenant(TenantConfigContext defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    void setTenantConfigContextFactory(Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory) {
        this.tenantConfigContextFactory = tenantConfigContextFactory;
    }

    private TenantConfigContext getTenantConfigFromTenantResolver(RoutingContext context) {
        String tenant = null;

        if (tenantResolver.isResolvable()) {
            tenant = tenantResolver.get().resolve(context);
        }

        return tenantsConfig.getOrDefault(tenant, defaultTenant);
    }

    boolean isBlocking(RoutingContext context) {
        return getTenantConfigFromConfigResolver(context, false) == null;
    }

    private TenantConfigContext getTenantConfigFromConfigResolver(RoutingContext context, boolean create) {
        if (tenantConfigResolver.isResolvable()) {
            OidcTenantConfig tenantConfig;

            if (context.get(CURRENT_TENANT_CONFIG) != null) {
                tenantConfig = context.get(CURRENT_TENANT_CONFIG);
            } else {
                tenantConfig = this.tenantConfigResolver.get().resolve(context);
                context.put(CURRENT_TENANT_CONFIG, tenantConfig);
            }

            if (tenantConfig != null) {
                String tenantId = tenantConfig.getTenantId()
                        .orElseThrow(() -> new IllegalStateException("You must provide a tenant id"));
                TenantConfigContext tenantContext = tenantsConfig.get(tenantId);

                if (tenantContext == null && create) {
                    synchronized (this) {
                        return tenantsConfig.computeIfAbsent(tenantId,
                                clientId -> tenantConfigContextFactory.apply(tenantConfig));
                    }
                }

                return tenantContext;
            }
        }

        return null;
    }
}
