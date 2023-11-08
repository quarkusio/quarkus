package io.quarkus.grpc.runtime.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.DefaultConverter;
import io.vertx.core.http.ClientAuth;

/**
 * Shared configuration for setting up server-side SSL.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class SslServerConfig {
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
     * An optional keystore that holds the certificate information instead of specifying separate files.
     * The keystore can be either on classpath or an external file.
     */
    @ConfigItem
    public Optional<Path> keyStore;

    /**
     * An optional parameter to specify the type of the keystore file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> keyStoreType;

    /**
     * A parameter to specify the password of the keystore file. If not given, the default ("password") is used.
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * An optional trust store which holds the certificate information of the certificates to trust
     * <p>
     * The trust store can be either on classpath or an external file.
     */
    @ConfigItem
    public Optional<Path> trustStore;

    /**
     * An optional parameter to specify type of the trust store file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> trustStoreType;

    /**
     * A parameter to specify the password of the trust store file.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * The cipher suites to use. If none is given, a reasonable default is selected.
     */
    @ConfigItem
    public Optional<List<String>> cipherSuites;

    /**
     * Sets the ordered list of enabled SSL/TLS protocols.
     * <p>
     * If not set, it defaults to {@code "TLSv1.3, TLSv1.2"}.
     * The following list of protocols are supported: {@code TLSv1, TLSv1.1, TLSv1.2, TLSv1.3}.
     * To only enable {@code TLSv1.3}, set the value to {@code to "TLSv1.3"}.
     * <p>
     * Note that setting an empty list, and enabling SSL/TLS is invalid.
     * You must at least have one protocol.
     */
    @DefaultConverter
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public Set<String> protocols;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(defaultValue = "NONE")
    public ClientAuth clientAuth;

}
