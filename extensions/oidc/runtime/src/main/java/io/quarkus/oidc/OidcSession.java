package io.quarkus.oidc;

import java.time.Instant;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.mutiny.Uni;

public interface OidcSession {

    /**
     * Return the tenant identifier of the current session
     *
     * @return tenant id
     */
    String getTenantId();

    /**
     * Return an {@linkplain:Instant} indicating how long will it take for the current session to expire.
     *
     * @return
     */
    Instant expiresIn();

    /**
     * Perform a local logout without a redirect to the OpenId Connect provider.
     *
     * @return Uni<Void>
     */
    Uni<Void> logout();

    /**
     * Return the ID token the current session depends upon.
     *
     * @return id token
     */
    JsonWebToken getIdToken();

}
