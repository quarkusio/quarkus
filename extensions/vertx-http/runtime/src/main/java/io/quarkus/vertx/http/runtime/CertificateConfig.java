package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * A certificate configuration.
 * Provide either the certificate and key files or a keystore.
 */
public interface CertificateConfig {
    /**
     * The {@linkplain CredentialsProvider}.
     * If this property is configured, then a matching 'CredentialsProvider' will be used
     * to get the keystore, keystore key, and truststore passwords unless these passwords have already been configured.
     * <p>
     * Please note that using MicroProfile {@linkplain ConfigSource} which is directly supported by Quarkus Configuration
     * should be preferred unless using `CredentialsProvider` provides for some additional security and dynamism.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> credentialsProvider();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> credentialsProviderName();

    /**
     * The list of path to server certificates using the PEM format.
     * Specifying multiple files requires SNI to be enabled.
     */
    Optional<List<Path>> files();

    /**
     * The list of path to server certificates private key files using the PEM format.
     * Specifying multiple files requires SNI to be enabled.
     * <p>
     * The order of the key files must match the order of the certificates.
     */
    Optional<List<Path>> keyFiles();

    /**
     * An optional keystore that holds the certificate information instead of specifying separate files.
     */
    Optional<Path> keyStoreFile();

    /**
     * An optional parameter to specify the type of the keystore file.
     * If not given, the type is automatically detected based on the file name.
     */
    Optional<String> keyStoreFileType();

    /**
     * An optional parameter to specify a provider of the keystore file.
     * If not given, the provider is automatically detected based on the keystore file type.
     */
    Optional<String> keyStoreProvider();

    /**
     * A parameter to specify the password of the keystore file.
     * If not given, and if it can not be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> keyStorePassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password of the key
     * store file from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> keyStorePasswordKey();

    /**
     * An optional parameter to select a specific key in the keystore.
     * When SNI is disabled, and the keystore contains multiple
     * keys and no alias is specified; the behavior is undefined.
     *
     * @deprecated Use {@link #keyStoreAlias} instead.
     */
    @Deprecated
    Optional<String> keyStoreKeyAlias();

    /**
     * An optional parameter to select a specific key in the keystore.
     * When SNI is disabled, and the keystore contains multiple
     * keys and no alias is specified; the behavior is undefined.
     */
    Optional<String> keyStoreAlias();

    /**
     * An optional parameter to define the password for the key,
     * in case it is different from {@link #keyStorePassword}
     * If not given, it might be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     * @deprecated Use {@link #keyStoreAliasPassword} instead.
     */
    @Deprecated
    Optional<String> keyStoreKeyPassword();

    /**
     * An optional parameter to define the password for the key,
     * in case it is different from {@link #keyStorePassword}
     * If not given, it might be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> keyStoreAliasPassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password for the alias from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     * @deprecated Use {@link #keyStoreAliasPasswordKey} instead.
     */
    @Deprecated
    Optional<String> keyStoreKeyPasswordKey();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password for the alias from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> keyStoreAliasPasswordKey();

    /**
     * An optional trust store that holds the certificate information of the trusted certificates.
     */
    Optional<Path> trustStoreFile();

    /**
     * An optional list of trusted certificates using the PEM format.
     * If you pass multiple files, you must use the PEM format.
     */
    Optional<List<Path>> trustStoreFiles();

    /**
     * An optional parameter to specify the type of the trust store file.
     * If not given, the type is automatically detected based on the file name.
     */
    Optional<String> trustStoreFileType();

    /**
     * An optional parameter to specify a provider of the trust store file.
     * If not given, the provider is automatically detected based on the trust store file type.
     */
    Optional<String> trustStoreProvider();

    /**
     * A parameter to specify the password of the trust store file.
     * If not given, it might be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> trustStorePassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key,
     * which can be used to get the password of the trust store file from {@linkplain CredentialsProvider}.
     *
     * @see CertificateConfig#credentialsProvider()
     */
    Optional<String> trustStorePasswordKey();

    /**
     * An optional parameter to trust a single certificate from the trust store rather than trusting all certificates in the
     * store.
     */
    Optional<String> trustStoreCertAlias();

    /**
     * When set, the configured certificate will be reloaded after the given period.
     * Note that the certificate will be reloaded only if the file has been modified.
     * <p>
     * Also, the update can also occur when the TLS certificate is configured using paths (and not in-memory).
     * <p>
     * The reload period must be equal or greater than 30 seconds. If not set, the certificate will not be reloaded.
     * <p>
     * IMPORTANT: It's recommended to use the TLS registry to handle the certificate reloading.
     * </p>
     */
    Optional<Duration> reloadPeriod();
}
