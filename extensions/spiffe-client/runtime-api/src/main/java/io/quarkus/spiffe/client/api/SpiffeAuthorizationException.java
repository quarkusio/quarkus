package io.quarkus.spiffe.client.api;

/**
 * Thrown when the SPIRE Agent denies a JWT-SVID request because the workload is not authorized.
 * This typically means no SPIRE registration entry matches this workload, or the requested
 * {@link JwtSvidRequest#spiffeId()} is not granted to it.
 */
public final class SpiffeAuthorizationException extends Exception {

    public SpiffeAuthorizationException(String message) {
        super(message);
    }

    public SpiffeAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
