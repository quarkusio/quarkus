package io.quarkus.oidc;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
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
     *
     * @deprecated Use {@link #resolve(RoutingContext, TenantConfigRequestContext))} instead.
     */
    @Deprecated
    default OidcTenantConfig resolve(RoutingContext context) {
        throw new UnsupportedOperationException("resolve is not implemented");
    }

    /**
     * Returns a {@link OidcTenantConfig} given a {@code RoutingContext}.
     *
     * @param requestContext the routing context
     * @return the tenant configuration. If the uni resolves to {@code null}, indicates that the default configuration/tenant
     *         should be chosen
     */
    default Uni<OidcTenantConfig> resolve(RoutingContext routingContext, TenantConfigRequestContext requestContext) {
        return Uni.createFrom().item(resolve(routingContext));
    }

    /**
     * A context object that can be used to run blocking tasks.
     * <p>
     * Blocking {@code TenantConfigResolver} providers should use this context object to run blocking tasks, to prevent
     * excessive and unnecessary delegation to thread pools.
     */
    interface TenantConfigRequestContext {

        Uni<OidcTenantConfig> runBlocking(Supplier<OidcTenantConfig> function);

    }

}
