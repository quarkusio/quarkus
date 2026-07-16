package io.quarkus.spiffe.client;

/**
 * Thrown when the SPIRE Agent denies a JWT-SVID request because the workload is not authorized.
 * This typically means no SPIRE registration entry matches this workload.
 */
public final class SpiffeAuthorizationException extends Exception {

    public SpiffeAuthorizationException(String message) {
        super(message);
    }

    public SpiffeAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
