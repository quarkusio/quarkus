package io.quarkus.keycloak.pep;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.vertx.ext.web.RoutingContext;

public interface PolicyEnforcerConfigResolver {
    /**
     * Returns a {@link KeycloakPolicyEnforcerTenantConfig} given a {@code RoutingContext}.
     *
     * @param context the routing context
     * @return the policy enforcer configuration. If {@code null}, indicates that the default configuration/tenant should be
     *         used
     */
    KeycloakPolicyEnforcerTenantConfig resolve(RoutingContext context);
}
