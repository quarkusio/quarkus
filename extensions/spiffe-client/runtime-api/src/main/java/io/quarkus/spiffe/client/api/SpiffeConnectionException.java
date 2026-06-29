package io.quarkus.spiffe.client.api;

/**
 * Thrown when a JWT-SVID cannot be obtained from the SPIRE Agent. Common causes include
 * a missing or misconfigured endpoint socket, the agent not running, a network timeout,
 * an unexpected gRPC error, or an invalid response (for example a JWT-SVID missing the
 * required {@code aud} claim).
 */
public final class SpiffeConnectionException extends Exception {

    public SpiffeConnectionException(String message) {
        super(message);
    }

    public SpiffeConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
