package io.quarkus.keycloak.pep;

import java.util.function.Supplier;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A tenant resolver is responsible for resolving the {@link KeycloakPolicyEnforcerTenantConfig} for tenants, dynamically.
 */
public interface TenantPolicyConfigResolver {

    /**
     * Returns a {@link KeycloakPolicyEnforcerTenantConfig} given a {@code RoutingContext} and tenant id.
     *
     * @param routingContext the routing context
     * @param tenantId tenant id
     * @param requestContext request context
     *
     * @return the tenant configuration. If the uni resolves to {@code null}, indicates that the default
     *         configuration/tenant should be chosen
     */
    Uni<KeycloakPolicyEnforcerTenantConfig> resolve(RoutingContext routingContext, String tenantId,
            KeycloakRequestContext requestContext);

    /**
     * Keycloak Context that can be used to run blocking tasks.
     */
    interface KeycloakRequestContext {
        <T> Uni<T> runBlocking(Supplier<T> function);
    }

}
