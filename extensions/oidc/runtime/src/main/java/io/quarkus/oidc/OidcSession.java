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
     * Return an {@linkplain:Instant} indicating how long will it take for the current session to expire.
     *
     * @deprecated This method shouldn't be used as it provides an instant corresponding to 1970-01-01T0:0:0Z plus the duration
     *             of the validity of the token, which is impractical. Please use either {@link #expiresAt()} or
     *             {@link #validFor()} depending on your requirements. This method will be removed in a later version of
     *             Quarkus.
     *
     * @return Instant
     */
    @Deprecated(forRemoval = true, since = "2.12.0")
    Instant expiresIn();

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
