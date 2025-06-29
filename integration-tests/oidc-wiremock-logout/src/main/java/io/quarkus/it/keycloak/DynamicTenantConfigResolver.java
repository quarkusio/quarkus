package io.quarkus.it.keycloak;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DynamicTenantConfigResolver implements TenantConfigResolver {

    private final Set<String> createdTenants = ConcurrentHashMap.newKeySet();

    @Inject
    OidcConfig oidcConfig;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext, OidcRequestContext<OidcTenantConfig> requestContext) {
        String normalizedPath = routingContext.normalizedPath();
        if (normalizedPath.contains("dynamic-tenant-")) {
            String tenantId = normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1);
            if (!createdTenants.contains(tenantId)) {
                var oidcTenantConfigBuilder = OidcTenantConfig.builder(oidcConfig.namedTenants().get("code-flow-form-post"));
                oidcTenantConfigBuilder.tenantId(tenantId);
                oidcTenantConfigBuilder.logout().backchannel().path("/back-channel-logout/" + tenantId).endLogout();
                oidcTenantConfigBuilder.tenantPaths("/service/" + tenantId, "/service/back-channel-logout/" + tenantId);
                Log.info("Created dynamic tenant config for tenant " + tenantId);
                createdTenants.add(tenantId);
                return Uni.createFrom().item(oidcTenantConfigBuilder.build());
            }
        }
        return Uni.createFrom().nullItem();
    }
}
