package io.quarkus.oidc.runtime;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;

public class TenantConfigBean {

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
        if (oidcConfig.logout.backchannel.path.isPresent()) {
            throw new ConfigurationException(
                    "BackChannel Logout is currently not supported for dynamic tenants");
        }
        var tenantId = oidcConfig.getTenantId().orElseThrow();
        if (!dynamicTenantsConfig.containsKey(tenantId)) {
            Uni<TenantConfigContext> uniContext = tenantContextFactory.create(oidcConfig, dynamicTenant, tenantId);
            return uniContext.onItem().transform(
                    new Function<TenantConfigContext, TenantConfigContext>() {
                        @Override
                        public TenantConfigContext apply(TenantConfigContext t) {
                            dynamicTenantsConfig.putIfAbsent(tenantId, t);
                            return t;
                        }
                    });
        } else {
            return Uni.createFrom().item(dynamicTenantsConfig.get(tenantId));
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
