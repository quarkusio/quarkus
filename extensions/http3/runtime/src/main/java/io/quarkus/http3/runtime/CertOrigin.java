package io.quarkus.http3.runtime;

/**
 * Enum indicating from where comes the certificate we are using to expose the HTTP/3 server.
 */
public enum CertOrigin {
    /**
     * Indicate that the used certificate has been signed by the Quarkus Dev CA.
     * This is the option used if TLS (for the HTTP server) is not configured and the Quarkus Dev CA is installed on
     * the system.
     */
    DEV_CA,
    /**
     * Indicate that the used certificate has been self-signed (no CA).
     * This is the option used if TLS (for the HTTP server) is not configured and the Quarkus Dev CA is not installed
     * on the system.
     */
    SELF_SIGNED,
    /**
     * Indicate that the used certificate has been configured by the user.
     * This is the option used if TLS (for the HTTP server) is configured.
     */
    CONFIGURED
}
