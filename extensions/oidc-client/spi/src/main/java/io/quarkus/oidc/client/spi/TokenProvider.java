package io.quarkus.oidc.client.spi;

import io.smallrye.mutiny.Uni;

/**
 * TokenProvider represents an OIDC client which can acquire and refresh
 * access tokens using the configured token grant properties.
 *
 * Use it when no user participation or dynamic input is required to complete
 * the token grant exchange.
 *
 * For example, you can use it with the client credentials, password,
 * JWT bearer and other similar grants, but not the authorization code flow grant.
 */
public interface TokenProvider {

    /**
     * Get a valid, if necessary refreshed, access token.
     *
     * @return the access token
     */
    Uni<String> getAccessToken();
}
