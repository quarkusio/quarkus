package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SslConfig {

    /**
     * The SSL protocol used to generate the SSLContext
     */
    @ConfigItem
    public Optional<String> protocol;

    /**
     * The name of the security provider used for SSL connections
     */
    @ConfigItem
    public Optional<String> provider;

    /**
     * A list of cipher suites
     */
    @ConfigItem
    public Optional<String> cipherSuites;

    /**
     * The list of protocols enabled for SSL connections
     */
    @ConfigItem
    public Optional<String> enabledProtocols;

    /**
     * Truststore config
     */
    public TrustStoreConfig truststore;

    /**
     * Keystore config
     */
    public KeyStoreConfig keystore;

    /**
     * Key config
     */
    public KeyConfig key;

    /**
     * The algorithm used by key manager factory for SSL connections
     */
    @ConfigItem
    public Optional<String> keymanagerAlgorithm;

    /**
     * The algorithm used by trust manager factory for SSL connections
     */
    @ConfigItem
    public Optional<String> trustmanagerAlgorithm;

    /**
     * The endpoint identification algorithm to validate server hostname using server certificate
     */
    @ConfigItem(defaultValue = "https")
    public Optional<String> endpointIdentificationAlgorithm;

    /**
     * The SecureRandom PRNG implementation to use for SSL cryptography operations
     */
    @ConfigItem
    public Optional<String> secureRandomImplementation;
}
