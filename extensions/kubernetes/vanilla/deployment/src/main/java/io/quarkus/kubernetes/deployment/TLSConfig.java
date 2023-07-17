package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TLSConfig {
    /**
     * The cert authority certificate contents.
     */
    @ConfigItem
    Optional<String> caCertificate;

    /**
     * The certificate contents.
     */
    @ConfigItem
    Optional<String> certificate;

    /**
     * The contents of the ca certificate of the final destination.
     */
    @ConfigItem
    Optional<String> destinationCACertificate;

    /**
     * The desired behavior for insecure connections to a route.
     */
    @ConfigItem
    Optional<String> insecureEdgeTerminationPolicy;

    /**
     * The key file contents.
     */
    @ConfigItem
    Optional<String> key;

    /**
     * The termination type.
     */
    @ConfigItem
    Optional<String> termination;
}
