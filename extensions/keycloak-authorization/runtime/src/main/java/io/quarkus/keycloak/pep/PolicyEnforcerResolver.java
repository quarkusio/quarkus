package io.quarkus.keycloak.pep;

import org.keycloak.adapters.authorization.PolicyEnforcer;

import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A {@link PolicyEnforcer} resolver.
 */
public interface PolicyEnforcerResolver {

    Uni<PolicyEnforcer> resolvePolicyEnforcer(RoutingContext routingContext, OidcTenantConfig tenantConfig);

    long getReadTimeout();

}
