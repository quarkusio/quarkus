package io.quarkus.grpc.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Shared configuration for setting up client-side SSL.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class SslClientConfig {
    /**
     * The classpath path or file path to a server certificate or certificate chain in PEM format.
     */
    @ConfigItem
    public Optional<Path> certificate;

    /**
     * The classpath path or file path to the corresponding certificate private key file in PEM format.
     */
    @ConfigItem
    public Optional<Path> key;

    /**
     * An optional trust store which holds the certificate information of the certificates to trust
     *
     * The trust store can be either on classpath or in an external file.
     */
    @ConfigItem
    public Optional<Path> trustStore;

}
