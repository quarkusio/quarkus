package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

/**
 * A certificate configuration.
 * Provide either the certificate and key files or a keystore.
 */
@ConfigGroup
public class CertificateConfig {

    /**
     * The {@linkplain CredentialsProvider}.
     * If this property is configured, then a matching 'CredentialsProvider' will be used
     * to get the keystore, keystore key, and truststore passwords unless these passwords have already been configured.
     * <p>
     * Please note that using MicroProfile {@linkplain ConfigSource} which is directly supported by Quarkus Configuration
     * should be preferred unless using `CredentialsProvider` provides for some additional security and dynamism.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProviderName = Optional.empty();

    /**
     * The list of path to server certificates using the PEM format.
     * Specifying multiple files requires SNI to be enabled.
     */
    @ConfigItem
    public Optional<List<Path>> files;

    /**
     * The list of path to server certificates private key files using the PEM format.
     * Specifying multiple files requires SNI to be enabled.
     * <p>
     * The order of the key files must match the order of the certificates.
     */
    @ConfigItem
    public Optional<List<Path>> keyFiles;

    /**
     * An optional keystore that holds the certificate information instead of specifying separate files.
     */
    @ConfigItem
    public Optional<Path> keyStoreFile;

    /**
     * An optional parameter to specify the type of the keystore file.
     * If not given, the type is automatically detected based on the file name.
     */
    @ConfigItem
    public Optional<String> keyStoreFileType;

    /**
     * An optional parameter to specify a provider of the keystore file.
     * If not given, the provider is automatically detected based on the keystore file type.
     */
    @ConfigItem
    public Optional<String> keyStoreProvider;

    /**
     * A parameter to specify the password of the keystore file.
     * If not given, and if it can not be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem(defaultValueDocumentation = "password")
    public Optional<String> keyStorePassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password of the key
     * store file from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> keyStorePasswordKey;

    /**
     * An optional parameter to select a specific key in the keystore.
     * When SNI is disabled, and the keystore contains multiple
     * keys and no alias is specified; the behavior is undefined.
     */
    @ConfigItem
    public Optional<String> keyStoreKeyAlias;

    /**
     * An optional parameter to define the password for the key,
     * in case it is different from {@link #keyStorePassword}
     * If not given, it might be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    @ConfigItem
    public Optional<String> keyStoreKeyPassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password for the key from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> keyStoreKeyPasswordKey;

    /**
     * An optional trust store that holds the certificate information of the trusted certificates.
     */
    @ConfigItem
    public Optional<Path> trustStoreFile;

    /**
     * An optional parameter to specify the type of the trust store file.
     * If not given, the type is automatically detected based on the file name.
     */
    @ConfigItem
    public Optional<String> trustStoreFileType;

    /**
     * An optional parameter to specify a provider of the trust store file.
     * If not given, the provider is automatically detected based on the trust store file type.
     */
    @ConfigItem
    public Optional<String> trustStoreProvider;

    /**
     * A parameter to specify the password of the trust store file.
     * If not given, it might be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password of the trust store file from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> trustStorePasswordKey;

    /**
     * An optional parameter to trust a single certificate from the trust store rather than trusting all certificates in the
     * store.
     */
    @ConfigItem
    public Optional<String> trustStoreCertAlias;

    /**
     * When set, the configured certificate will be reloaded after the given period.
     * Note that the certificate will be reloaded only if the file has been modified.
     * <p>
     * Also, the update can also occur when the TLS certificate is configured using paths (and not in-memory).
     * <p>
     * The reload period must be equal or greater than 30 seconds. If not set, the certificate will not be reloaded.
     */
    @ConfigItem
    public Optional<Duration> reloadPeriod;
}
