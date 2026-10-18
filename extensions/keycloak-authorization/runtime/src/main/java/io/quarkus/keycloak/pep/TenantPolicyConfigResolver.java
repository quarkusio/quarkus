package io.quarkus.keycloak.pep;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A tenant resolver is responsible for resolving the {@link KeycloakPolicyEnforcerTenantConfig} for tenants, dynamically.
 */
public interface TenantPolicyConfigResolver {

    /**
     * Returns a {@link KeycloakPolicyEnforcerTenantConfig} given a {@code RoutingContext} and tenant id.
     *
     * @param routingContext routing context; nullable
     * @param tenantConfig tenant config; never null
     * @param requestContext request context; never null
     *
     * @return the tenant configuration. If the uni resolves to {@code null}, indicates that the default
     *         configuration/tenant should be chosen
     */
    Uni<KeycloakPolicyEnforcerTenantConfig> resolve(RoutingContext routingContext, OidcTenantConfig tenantConfig,
            OidcRequestContext<KeycloakPolicyEnforcerTenantConfig> requestContext);

}
