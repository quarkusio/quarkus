package io.quarkus.oidc;

import java.time.Duration;
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
     * Return an {@linkplain Instant} representing the current session's expiration time.
     *
     * @return Instant
     */
    Instant expiresAt();

    /**
     * Return a {@linkplain Duration} indicating how long the current session will remain valid for
     * starting from this method's invocation time.
     *
     * @return Duration
     */
    Duration validFor();

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
