package io.quarkus.oidc.runtime;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;

public class TenantConfigBean {
    /*
     * Note: this class is publicly documented on https://quarkus.io/guides/security-oidc-code-flow-authentication.
     */

    private static final Logger LOG = Logger.getLogger(TenantConfigBean.class);

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final TenantContextFactory tenantContextFactory;

    @FunctionalInterface
    public interface TenantContextFactory {
        Uni<TenantConfigContext> create(OidcTenantConfig oidcTenantConfig, boolean dynamicTenant, String tenantId);
    }

    TenantConfigBean(
            Map<String, TenantConfigContext> staticTenantsConfig,
            TenantConfigContext defaultTenant,
            TenantContextFactory tenantContextFactory) {
        this.staticTenantsConfig = new ConcurrentHashMap<>(staticTenantsConfig);
        this.dynamicTenantsConfig = new ConcurrentHashMap<>();
        this.defaultTenant = defaultTenant;
        this.tenantContextFactory = tenantContextFactory;
    }

    Uni<TenantConfigContext> getOrCreateTenantContext(OidcTenantConfig oidcConfig, boolean dynamicTenant) {
        var tenantId = oidcConfig.getTenantId().orElseThrow();
        var tenants = dynamicTenant ? dynamicTenantsConfig : staticTenantsConfig;
        var tenant = tenants.get(tenantId);
        if (tenant == null || !tenant.ready) {
            LOG.tracef("Creating %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
            if (dynamicTenant && oidcConfig.logout.backchannel.path.isPresent()) {
                throw new ConfigurationException(
                        "BackChannel Logout is currently not supported for dynamic tenants (tenant ID: " + tenantId + ")");
            }
            Uni<TenantConfigContext> uniContext = tenantContextFactory.create(oidcConfig, dynamicTenant, tenantId);
            return uniContext.onItem().transform(
                    new Function<TenantConfigContext, TenantConfigContext>() {
                        @Override
                        public TenantConfigContext apply(TenantConfigContext t) {
                            LOG.debugf("Updating %s %s tenant config for %s", dynamicTenant ? "dynamic" : "static",
                                    t.ready ? "ready" : "not-ready", tenantId);
                            tenants.put(tenantId, t);
                            return t;
                        }
                    });
        }
        LOG.tracef("Immediately returning ready %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
        return Uni.createFrom().item(tenant);
    }

    /**
     * Returns a static tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getStaticTenantConfigContext(String tenantId) {
        return staticTenantsConfig.get(tenantId);
    }

    /**
     * Returns a static tenant's OIDC configuration or {@code null}, if the tenant does not exist.
     */
    public OidcTenantConfig getStaticTenantOidcConfig(String tenantId) {
        var context = getStaticTenantConfigContext(tenantId);
        return context != null ? context.oidcConfig : null;
    }

    /**
     * Returns an unmodifiable map containing the static tenant config contexts by tenant-ID.
     */
    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return unmodifiableMap(staticTenantsConfig);
    }

    /**
     * Returns a dynamic tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getDynamicTenantConfigContext(String tenantId) {
        return dynamicTenantsConfig.get(tenantId);
    }

    /**
     * Returns a dynamic tenant's OIDC configuration or {@code null}, if the tenant does not exist.
     */
    public OidcTenantConfig getDynamicTenantOidcConfig(String tenantId) {
        var context = getDynamicTenantConfigContext(tenantId);
        return context != null ? context.oidcConfig : null;
    }

    /**
     * Returns an unmodifiable map containing the dynamic tenant config contexts by tenant-ID.
     */
    public Map<String, TenantConfigContext> getDynamicTenantsConfig() {
        return unmodifiableMap(dynamicTenantsConfig);
    }

    /**
     * Returns the default tenant's config context.
     */
    public TenantConfigContext getDefaultTenant() {
        return defaultTenant;
    }

    public static class Destroyer implements BeanDestroyer<TenantConfigBean> {

        @Override
        public void destroy(TenantConfigBean instance, CreationalContext<TenantConfigBean> creationalContext,
                Map<String, Object> params) {
            if (instance.defaultTenant != null && instance.defaultTenant.provider != null) {
                instance.defaultTenant.provider.close();
            }
            for (var i : instance.staticTenantsConfig.values()) {
                if (i.provider != null) {
                    i.provider.close();
                }
            }
            for (var i : instance.dynamicTenantsConfig.values()) {
                if (i.provider != null) {
                    i.provider.close();
                }
            }
        }
    }
}
