package io.quarkus.it.keycloak;

import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.keycloak.pep.TenantPolicyConfigResolver;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@IfBuildProfile("dynamic-config-resolver")
@ApplicationScoped
public class DynamicTenantPolicyConfigResolver implements TenantPolicyConfigResolver {

    private final KeycloakPolicyEnforcerTenantConfig enhancedTenantConfig;
    private final KeycloakPolicyEnforcerTenantConfig newTenantConfig;

    public DynamicTenantPolicyConfigResolver(KeycloakPolicyEnforcerConfig enforcerConfig) {
        var builder = KeycloakPolicyEnforcerTenantConfig.builder(enforcerConfig.defaultTenant());
        var path = builder.setPaths("/api/permission/scopes/dynamic-way");
        path.setPermissionName("Scope Permission Resource");
        path.setGet("read");
        path = builder.setPaths("/api/permission/scopes/dynamic-way-denied");
        path.setPermissionName("Scope Permission Resource");
        path.setGet("write");
        this.enhancedTenantConfig = builder.build();
        builder = KeycloakPolicyEnforcerTenantConfig.builder();
        path = builder.setPaths("/dynamic-permission-tenant");
        path.setPermissionName("Dynamic Config Permission Resource Tenant");
        path.setClaimInformationPoint(Map.of("claims", Map.of("static-claim", "static-claim")));
        this.newTenantConfig = builder.build();
    }

    @Override
    public Uni<KeycloakPolicyEnforcerTenantConfig> resolve(RoutingContext routingContext, String tenantId,
            KeycloakRequestContext requestContext) {
        String path = routingContext.normalizedPath();
        if (DEFAULT_TENANT_ID.equals(tenantId) && path.startsWith("/api/permission/scopes/dynamic-way")) {
            return Uni.createFrom().item(enhancedTenantConfig);
        } else if ("api-permission-tenant".equals(tenantId) && path.equals("/dynamic-permission-tenant")) {
            return Uni.createFrom().item(newTenantConfig);
        }
        return Uni.createFrom().nullItem();
    }
}
