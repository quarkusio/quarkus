package io.quarkus.oidc;

import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.vertx.ext.web.RoutingContext;

/**
 * <p>
 * A tenant resolver is responsible for resolving the {@link OidcTenantConfig} for tenants, dynamically.
 * 
 * <p>
 * Instead of implementing a {@link TenantResolver} that maps the tenant configuration based on an identifier and its
 * corresponding entry in the application configuration file, beans implementing this interface can dynamically construct the
 * tenant configuration without having to define each tenant in the application configuration file.
 */
public interface TenantConfigResolver {

    /**
     * Returns a {@link OidcTenantConfig} given a {@code RoutingContext}.
     *
     * @param context the routing context
     * @return the tenant configuration. If {@code null}, indicates that the default configuration/tenant should be chosen
     */
    OidcTenantConfig resolve(RoutingContext context);
}
