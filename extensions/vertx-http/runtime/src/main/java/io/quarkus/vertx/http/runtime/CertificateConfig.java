package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A certificate configuration. Either the certificate and key files must be given, or a key store must be given.
 */
@ConfigGroup
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CertificateConfig {

    /**
     * The file path to a server certificate or certificate chain in PEM format.
     *
     * @deprecated Use {@link #files} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Path> file;

    /**
     * The list of path to server certificates using the PEM format.
     * Specifying multiple files require SNI to be enabled.
     */
    @ConfigItem
    public Optional<List<Path>> files;

    /**
     * The file path to the corresponding certificate private key file in PEM format.
     *
     * @deprecated Use {@link #keyFiles} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Path> keyFile;

    /**
     * The list of path to server certificates private key file using the PEM format.
     * Specifying multiple files require SNI to be enabled.
     *
     * The order of the key files must match the order of the certificates.
     */
    @ConfigItem
    public Optional<List<Path>> keyFiles;

    /**
     * An optional key store which holds the certificate information instead of specifying separate files.
     */
    @ConfigItem
    public Optional<Path> keyStoreFile;

    /**
     * An optional parameter to specify type of the key store file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> keyStoreFileType;

    /**
     * An optional parameter to specify a provider of the key store file. If not given, the provider is automatically detected
     * based on the key store file type.
     */
    @ConfigItem
    public Optional<String> keyStoreProvider;

    /**
     * A parameter to specify the password of the key store file. If not given, the default ("password") is used.
     */
    @ConfigItem(defaultValue = "password")
    public String keyStorePassword;

    /**
     * An optional parameter to select a specific key in the key store. When SNI is disabled, if the key store contains multiple
     * keys and no alias is specified, the behavior is undefined.
     */
    @ConfigItem
    public Optional<String> keyStoreKeyAlias;

    /**
     * An optional trust store which holds the certificate information of the certificates to trust.
     */
    @ConfigItem
    public Optional<Path> trustStoreFile;

    /**
     * An optional parameter to specify type of the trust store file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> trustStoreFileType;

    /**
     * An optional parameter to specify a provider of the trust store file. If not given, the provider is automatically detected
     * based on the trust store file type.
     */
    @ConfigItem
    public Optional<String> trustStoreProvider;

    /**
     * A parameter to specify the password of the trust store file.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * An optional parameter to trust only one specific certificate in the trust store (instead of trusting all certificates in
     * the store).
     */
    @ConfigItem
    public Optional<String> trustStoreCertAlias;
}
