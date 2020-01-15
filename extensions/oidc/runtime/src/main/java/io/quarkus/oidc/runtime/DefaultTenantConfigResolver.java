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

    @Inject
    Instance<TenantResolver> tenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    private Map<String, TenantConfigContext> tenantsConfig;
    private TenantConfigContext defaultTenant;
    private Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory;

    TenantConfigContext resolve(RoutingContext context) {
        if (tenantConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
        }

        if (tenantConfigResolver.isResolvable()) {
            OidcTenantConfig tenantConfig = this.tenantConfigResolver.get().resolve(context);

            if (tenantConfig != null) {
                String tenantId = tenantConfig.getClientId()
                        .orElseThrow(() -> new IllegalStateException("You must provide a client_id"));
                TenantConfigContext tenantContext = tenantsConfig.get(tenantId);

                if (tenantContext == null) {
                    synchronized (this) {
                        return tenantsConfig.computeIfAbsent(tenantId,
                                clientId -> tenantConfigContextFactory.apply(tenantConfig));
                    }
                }

                return tenantContext;
            }
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
}
