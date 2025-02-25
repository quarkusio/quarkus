package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public final class TenantConfigBean {

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final TenantContextFactory tenantContextFactory;

    TenantConfigBean(Vertx vertx, TlsConfigurationRegistry tlsConfigurationRegistry, OidcImpl oidc,
            boolean securityEventsEnabled) {
        this.tenantContextFactory = new TenantContextFactory(vertx, tlsConfigurationRegistry, securityEventsEnabled);
        this.dynamicTenantsConfig = new ConcurrentHashMap<>();

        this.staticTenantsConfig = tenantContextFactory.createStaticTenantConfigs(oidc.getStaticTenantConfigs(),
                oidc.getDefaultTenantConfig());
        this.defaultTenant = tenantContextFactory.createDefaultTenantConfig(oidc.getStaticTenantConfigs(),
                oidc.getDefaultTenantConfig());
    }

    public Uni<TenantConfigContext> createDynamicTenantContext(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId().orElseThrow();

        var tenant = dynamicTenantsConfig.get(tenantId);
        if (tenant != null) {
            return Uni.createFrom().item(tenant);
        }

        return tenantContextFactory.createDynamic(oidcConfig).onItem().transform(
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
