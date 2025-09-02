package io.quarkus.oidc.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public final class TenantConfigBean {

    private static final Logger LOG = Logger.getLogger(TenantConfigBean.class);

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

    Uni<TenantConfigContext> createDynamicTenantContext(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId().orElseThrow();

        var tenant = dynamicTenantsConfig.get(tenantId);
        if (tenant != null) {
            return Uni.createFrom().item(tenant);
        }

        return tenantContextFactory.createDynamic(oidcConfig).onItem().transform(
                new Function<TenantConfigContext, TenantConfigContext>() {
                    @Override
                    public TenantConfigContext apply(TenantConfigContext t) {
                        var previousValue = dynamicTenantsConfig.putIfAbsent(tenantId, t);
                        if (previousValue == null) {
                            BackChannelLogoutHandler.fireBackChannelLogoutReadyEvent(oidcConfig);
                            ResourceMetadataHandler.fireResourceMetadataReadyEvent(oidcConfig);
                        }
                        return t;
                    }
                });
    }

    Uni<TenantConfigContext> updateDynamicTenantContext(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId().orElseThrow();
        var tenant = dynamicTenantsConfig.get(tenantId);
        if (tenant != null) {
            LOG.debugf("Updating the resolved tenant %s configuration with a new configuration", tenantId);
            var newTenant = new TenantConfigContextImpl(tenant, oidcConfig);
            dynamicTenantsConfig.put(tenantId, newTenant);
            BackChannelLogoutHandler.fireBackChannelLogoutChangedEvent(oidcConfig, tenant);
            ResourceMetadataHandler.fireResourceMetadataChangedEvent(oidcConfig, tenant);
            return Uni.createFrom().item(newTenant);
        } else {
            return createDynamicTenantContext(oidcConfig);
        }
    }

    Uni<TenantConfigContext> replaceDynamicTenantContext(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId().orElseThrow();
        LOG.debugf("Replacing the resolved tenant %s configuration with a new configuration", tenantId);
        dynamicTenantsConfig.remove(tenantId);
        return createDynamicTenantContext(oidcConfig);
    }

    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return staticTenantsConfig;
    }

    List<TenantConfigContext> getAllTenantConfigs() {
        List<TenantConfigContext> result = new ArrayList<>();
        result.add(getDefaultTenant());
        result.addAll(getStaticTenantsConfig().values());
        result.addAll(dynamicTenantsConfig.values());
        return result;
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
