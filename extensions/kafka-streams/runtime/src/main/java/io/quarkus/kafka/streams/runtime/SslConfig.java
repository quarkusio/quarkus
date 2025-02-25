package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface SslConfig {

    /**
     * The SSL protocol used to generate the SSLContext
     */
    Optional<String> protocol();

    /**
     * The name of the security provider used for SSL connections
     */
    Optional<String> provider();

    /**
     * A list of cipher suites
     */
    Optional<String> cipherSuites();

    /**
     * The list of protocols enabled for SSL connections
     */
    Optional<String> enabledProtocols();

    /**
     * Truststore config
     */
    TrustStoreConfig truststore();

    /**
     * Keystore config
     */
    KeyStoreConfig keystore();

    /**
     * Key config
     */
    KeyConfig key();

    /**
     * The algorithm used by key manager factory for SSL connections
     */
    Optional<String> keymanagerAlgorithm();

    /**
     * The algorithm used by trust manager factory for SSL connections
     */
    Optional<String> trustmanagerAlgorithm();

    /**
     * The endpoint identification algorithm to validate server hostname using server certificate
     */
    @WithDefault("https")
    Optional<String> endpointIdentificationAlgorithm();

    /**
     * The SecureRandom PRNG implementation to use for SSL cryptography operations
     */
    Optional<String> secureRandomImplementation();
}
