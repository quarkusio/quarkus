package io.quarkus.oidc;

import io.smallrye.mutiny.Uni;

/**
 * Token introspection cache.
 */
public interface TokenIntrospectionCache {

    /**
     * Add a new {@link TokenIntrospection} result to the cache.
     *
     * @param token the token which has been introspected
     * @param introspection the token introspection result
     * @param oidcConfig the tenant configuration
     * @param requestContext the request context which can be used to run the blocking tasks
     */
    Uni<Void> addIntrospection(String token, TokenIntrospection introspection, OidcTenantConfig oidcConfig,
            OidcRequestContext<Void> requestContext);

    /**
     * Get the cached {@link TokenIntrospection} result.
     *
     * @param token the token which has to be introspected
     * @param oidcConfig the tenant configuration
     * @param requestContext the request context which can be used to run the blocking tasks
     */
    Uni<TokenIntrospection> getIntrospection(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<TokenIntrospection> requestContext);
}
