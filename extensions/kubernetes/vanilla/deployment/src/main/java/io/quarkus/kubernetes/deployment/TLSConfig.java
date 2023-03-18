package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public class TLSConfig {
    /**
     * @return the cert authority certificate contents.
     */
    Optional<String> caCertificate;

    /**
     * @return the certificate contents.
     */
    Optional<String> certificate;

    /**
     * @return the contents of the ca certificate of the final destination.
     */
    Optional<String> destinationCACertificate;

    /**
     * @return the desired behavior for insecure connections to a route. Options are: `allow`, `disable`, and `redirect`.
     */
    Optional<String> insecureEdgeTerminationPolicy;

    /**
     * @return the key file contents.
     */
    Optional<String> key;

    /**
     * @return the termination type.
     */
    Optional<String> termination;
}
