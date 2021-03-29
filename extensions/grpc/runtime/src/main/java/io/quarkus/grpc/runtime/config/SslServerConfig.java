package io.quarkus.grpc.runtime.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
     * An optional key store which holds the certificate information instead of specifying separate files.
     * The key store can be either on classpath or an external file.
     */
    @ConfigItem
    public Optional<Path> keyStore;

    /**
     * An optional parameter to specify the type of the key store file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> keyStoreType;

    /**
     * A parameter to specify the password of the key store file. If not given, the default ("password") is used.
     */
    @ConfigItem(defaultValue = "password")
    public String keyStorePassword;

    /**
     * An optional trust store which holds the certificate information of the certificates to trust
     *
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
     * The list of protocols to explicitly enable.
     */
    @DefaultConverter
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public List<String> protocols;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(defaultValue = "NONE")
    public ClientAuth clientAuth;

}
