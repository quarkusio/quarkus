package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTenantConfigResolver {

    private static final Logger LOG = Logger.getLogger(DefaultTenantConfigResolver.class);
    private static final String CURRENT_TENANT_CONFIG = "io.quarkus.oidc.current.tenant.config";

    @Inject
    Instance<TenantResolver> tenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    private final Map<String, TenantConfigContext> dynamicTenantsConfig = new ConcurrentHashMap<>();

    @Inject
    TenantConfigBean tenantConfigBean;

    @PostConstruct
    public void verifyResolvers() {
        if (tenantConfigResolver.isResolvable()) {
            if (tenantConfigResolver.isAmbiguous()) {
                throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
            }
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

    private TenantConfigContext getTenantConfigFromTenantResolver(RoutingContext context) {

        String tenantId = null;

        if (tenantResolver.isResolvable()) {
            tenantId = tenantResolver.get().resolve(context);
        }

        TenantConfigContext configContext = tenantId != null ? tenantConfigBean.getStaticTenantsConfig().get(tenantId) : null;
        if (configContext == null) {
            if (tenantId != null && !tenantId.isEmpty()) {
                LOG.debugf("No configuration with a tenant id '%s' has been found, using the default configuration");
            }
            configContext = tenantConfigBean.getDefaultTenant();
        }
        return configContext;
    }

    boolean isBlocking(RoutingContext context) {
        TenantConfigContext resolver = getTenantConfigFromConfigResolver(context, false);

        if (resolver != null) {
            // we always run blocking if refresh token is enabled even if the tenant was already resolved
            return resolver.oidcConfig.token.refreshExpired;
        }

        return resolver == null;
    }

    private TenantConfigContext getTenantConfigFromConfigResolver(RoutingContext context, boolean create) {
        if (tenantConfigResolver.isResolvable()) {
            OidcTenantConfig tenantConfig;

            if (context.get(CURRENT_TENANT_CONFIG) != null) {
                tenantConfig = context.get(CURRENT_TENANT_CONFIG);
            } else {
                OidcTenantConfig newTenantConfig = this.tenantConfigResolver.get().resolve(context);
                if (newTenantConfig != null && !newTenantConfig.tenantEnabled) {
                    newTenantConfig = null;
                }
                tenantConfig = newTenantConfig;
                context.put(CURRENT_TENANT_CONFIG, tenantConfig);
            }

            if (tenantConfig != null) {
                String tenantId = tenantConfig.getTenantId()
                        .orElseThrow(() -> new IllegalStateException("You must provide a tenant id"));
                TenantConfigContext tenantContext = dynamicTenantsConfig.get(tenantId);

                if (tenantContext == null && create) {
                    synchronized (dynamicTenantsConfig) {
                        return dynamicTenantsConfig.computeIfAbsent(tenantId,
                                clientId -> tenantConfigBean.getTenantConfigContextFactory().apply(tenantConfig));
                    }
                }

                return tenantContext;
            }
        }

        return null;
    }
}
