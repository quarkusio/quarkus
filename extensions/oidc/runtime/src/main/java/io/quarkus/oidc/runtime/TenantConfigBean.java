package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;

public class TenantConfigBean {

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final TenantContextFactory tenantContextFactory;

    @FunctionalInterface
    public interface TenantContextFactory {
        Uni<TenantConfigContext> create(OidcTenantConfig oidcTenantConfig);
    }

    public TenantConfigBean(
            Map<String, TenantConfigContext> staticTenantsConfig,
            TenantConfigContext defaultTenant,
            TenantContextFactory tenantContextFactory) {
        this.staticTenantsConfig = Map.copyOf(staticTenantsConfig);
        this.dynamicTenantsConfig = new ConcurrentHashMap<>();
        this.defaultTenant = defaultTenant;
        this.tenantContextFactory = tenantContextFactory;
    }

    public Uni<TenantConfigContext> createDynamicTenantContext(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId.orElseThrow();

        var tenant = dynamicTenantsConfig.get(tenantId);
        if (tenant != null) {
            return Uni.createFrom().item(tenant);
        }

        return tenantContextFactory.create(oidcConfig).onItem().transform(
                new Function<TenantConfigContext, TenantConfigContext>() {
                    @Override
                    public TenantConfigContext apply(TenantConfigContext t) {
                        dynamicTenantsConfig.putIfAbsent(tenantId, t);
                        return t;
                    }
                });
    }

    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return staticTenantsConfig;
    }

    public TenantConfigContext getStaticTenant(String tenantId) {
        return staticTenantsConfig.get(tenantId);
    }

    public TenantConfigContext getDefaultTenant() {
        return defaultTenant;
    }

    public TenantConfigContext getDynamicTenant(String tenantId) {
        return dynamicTenantsConfig.get(tenantId);
    }

    public static class Destroyer implements BeanDestroyer<TenantConfigBean> {

        @Override
        public void destroy(TenantConfigBean instance, CreationalContext<TenantConfigBean> creationalContext,
                Map<String, Object> params) {
            if (instance.defaultTenant != null && instance.defaultTenant.provider() != null) {
                instance.defaultTenant.provider().close();
            }
            for (var i : instance.staticTenantsConfig.values()) {
                if (i.provider() != null) {
                    i.provider().close();
                }
            }
            for (var i : instance.dynamicTenantsConfig.values()) {
                if (i.provider() != null) {
                    i.provider().close();
                }
            }
        }
    }
}
