package io.quarkus.oidc;

import io.vertx.ext.web.RoutingContext;

/**
 * A tenant resolver is responsible for resolving tenants dynamically so that the proper configuration can be used accordingly.
 */
public interface TenantResolver {

    /**
     * Returns a tenant identifier given a {@code RoutingContext}, where the identifier will be used to choose the proper
     * configuration during runtime.
     * 
     * @param context the routing context
     * @return the tenant identifier. If {@code null}, indicates that the default configuration/tenant should be chosen
     */
    String resolve(RoutingContext context);
}
