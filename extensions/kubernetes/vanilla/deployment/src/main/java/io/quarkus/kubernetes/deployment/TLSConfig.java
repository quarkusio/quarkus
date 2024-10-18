package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface TLSConfig {
    /**
     * The cert authority certificate contents.
     */
    Optional<String> caCertificate();

    /**
     * The certificate contents.
     */
    Optional<String> certificate();

    /**
     * The contents of the ca certificate of the final destination.
     */
    Optional<String> destinationCACertificate();

    /**
     * The desired behavior for insecure connections to a route.
     */
    Optional<String> insecureEdgeTerminationPolicy();

    /**
     * The key file contents.
     */
    Optional<String> key();

    /**
     * The termination type.
     */
    Optional<String> termination();
}
