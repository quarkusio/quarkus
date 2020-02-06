package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.function.Function;

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

    TenantConfigContext resolve(RoutingContext context) {
        if (tenantConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
        }

        TenantConfigContext config = getTenantConfigFromConfigResolver(context, true);

        if (config != null) {
            return config;
        }

        String tenant = null;

        if (tenantResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantResolver.class + " beans registered");
        }

        if (tenantResolver.isResolvable()) {
            tenant = tenantResolver.get().resolve(context);
        }

        return tenantsConfig.getOrDefault(tenant, defaultTenant);
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
                String tenantId = tenantConfig.getClientId()
                        .orElseThrow(() -> new IllegalStateException("You must provide a client_id"));
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
