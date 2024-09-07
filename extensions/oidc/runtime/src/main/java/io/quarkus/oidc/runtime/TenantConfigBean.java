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

    private static final Logger LOG = Logger.getLogger(TenantConfigBean.class);

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final TenantContextFactory tenantContextFactory;

    @FunctionalInterface
    public interface TenantContextFactory {
        Uni<TenantConfigContext> create(OidcTenantConfig oidcTenantConfig, boolean dynamicTenant, String tenantId);
    }

    public TenantConfigBean(
            Map<String, TenantConfigContext> staticTenantsConfig,
            TenantConfigContext defaultTenant,
            TenantContextFactory tenantContextFactory) {
        this.staticTenantsConfig = new ConcurrentHashMap<>(staticTenantsConfig);
        this.dynamicTenantsConfig = new ConcurrentHashMap<>();
        this.defaultTenant = defaultTenant;
        this.tenantContextFactory = tenantContextFactory;
    }

    public Uni<TenantConfigContext> createTenantContext(OidcTenantConfig oidcConfig, boolean dynamicTenant) {
        var tenantId = oidcConfig.getTenantId().orElseThrow();
        if (dynamicTenant && oidcConfig.logout.backchannel.path.isPresent()) {
            throw new ConfigurationException(
                    "BackChannel Logout is currently not supported for dynamic tenants (tenant ID: " + tenantId + ")");
        }
        var tenants = dynamicTenant ? dynamicTenantsConfig : staticTenantsConfig;
        var tenant = tenants.get(tenantId);
        if (tenant == null || !tenant.ready) {
            LOG.tracef("Creating %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
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
        } else {
            LOG.tracef("Immediately returning ready %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
            return Uni.createFrom().item(tenant);
        }
    }

    public TenantConfigContext getStaticTenant(String tenantId) {
        return staticTenantsConfig.get(tenantId);
    }

    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return unmodifiableMap(staticTenantsConfig);
    }

    public TenantConfigContext getDynamicTenant(String tenantId) {
        return dynamicTenantsConfig.get(tenantId);
    }

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
