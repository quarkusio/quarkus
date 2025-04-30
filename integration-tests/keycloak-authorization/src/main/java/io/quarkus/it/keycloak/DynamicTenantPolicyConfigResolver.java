package io.quarkus.it.keycloak;

import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.keycloak.pep.TenantPolicyConfigResolver;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@IfBuildProfile("dynamic-config-resolver")
@ApplicationScoped
public class DynamicTenantPolicyConfigResolver implements TenantPolicyConfigResolver {

    private final KeycloakPolicyEnforcerTenantConfig enhancedTenantConfig;
    private final KeycloakPolicyEnforcerTenantConfig newTenantConfig;

    public DynamicTenantPolicyConfigResolver(KeycloakPolicyEnforcerConfig enforcerConfig) {
        this.enhancedTenantConfig = KeycloakPolicyEnforcerTenantConfig.builder(enforcerConfig.defaultTenant())
                .paths("/api/permission/scopes/dynamic-way")
                .permissionName("Scope Permission Resource")
                .get("read")
                .paths("/api/permission/scopes/dynamic-way-denied")
                .permissionName("Scope Permission Resource")
                .get("write")
                .build();
        this.newTenantConfig = KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("/dynamic-permission-tenant")
                .permissionName("Dynamic Config Permission Resource Tenant")
                .claimInformationPoint(Map.of("claims", Map.of("static-claim", "static-claim")))
                .build();
    }

    @Override
    public Uni<KeycloakPolicyEnforcerTenantConfig> resolve(RoutingContext routingContext, OidcTenantConfig tenantConfig,
            OidcRequestContext<KeycloakPolicyEnforcerTenantConfig> requestContext) {
        String path = routingContext.normalizedPath();
        String tenantId = tenantConfig.tenantId.orElse(null);
        if (DEFAULT_TENANT_ID.equals(tenantId) && path.startsWith("/api/permission/scopes/dynamic-way")) {
            return Uni.createFrom().item(enhancedTenantConfig);
        } else if ("api-permission-tenant".equals(tenantId) && path.equals("/dynamic-permission-tenant")) {
            return Uni.createFrom().item(newTenantConfig);
        }
        return Uni.createFrom().nullItem();
    }
}
