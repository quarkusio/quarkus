package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * A certificate configuration. Either the certificate and key files must be given, or a key store must be given.
 */
public interface CertificateConfig {
    /**
     * The {@linkplain CredentialsProvider}.
     * If this property is configured then a matching 'CredentialsProvider' will be used
     * to get the keystore, keystore key and truststore passwords unless these passwords have already been configured.
     *
     * Please note that using MicroProfile {@linkplain ConfigSource} which is directly supported by Quarkus Configuration
     * should be preferred unless using `CredentialsProvider` provides for some additional security and dynamism.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProvider();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * It is recommended to set this property even if there is only one credentials provider currently available
     * to ensure the same provider is always found in deployments where more than one provider may be available.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> credentialsProviderName();

    /**
     * The list of path to server certificates using the PEM format.
     * Specifying multiple files require SNI to be enabled.
     */
    Optional<List<Path>> files();

    /**
     * The list of path to server certificates private key file using the PEM format.
     * Specifying multiple files require SNI to be enabled.
     *
     * The order of the key files must match the order of the certificates.
     */
    Optional<List<Path>> keyFiles();

    /**
     * An optional key store which holds the certificate information instead of specifying separate files.
     */
    Optional<Path> keyStoreFile();

    /**
     * An optional parameter to specify type of the key store file. If not given, the type is automatically detected
     * based on the file name.
     */
    Optional<String> keyStoreFileType();

    /**
     * An optional parameter to specify a provider of the key store file. If not given, the provider is automatically detected
     * based on the key store file type.
     */
    Optional<String> keyStoreProvider();

    /**
     * A parameter to specify the password of the key store file. If not given, and if it can not be retrieved from
     * {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    Optional<String> keyStorePassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password of the key
     * store file
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    Optional<String> keyStorePasswordKey();

    /**
     * An optional parameter to select a specific key in the key store. When SNI is disabled, if the key store contains multiple
     * keys and no alias is specified, the behavior is undefined.
     */
    Optional<String> keyStoreKeyAlias();

    /**
     * An optional parameter to define the password for the key, in case it's different from {@link #keyStorePassword}
     * If not given then it may be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    Optional<String> keyStoreKeyPassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password for the key
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    Optional<String> keyStoreKeyPasswordKey();

    /**
     * An optional trust store which holds the certificate information of the certificates to trust.
     */
    Optional<Path> trustStoreFile();

    /**
     * An optional parameter to specify type of the trust store file. If not given, the type is automatically detected
     * based on the file name.
     */
    Optional<String> trustStoreFileType();

    /**
     * An optional parameter to specify a provider of the trust store file. If not given, the provider is automatically detected
     * based on the trust store file type.
     */
    Optional<String> trustStoreProvider();

    /**
     * A parameter to specify the password of the trust store file.
     * If not given then it may be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    Optional<String> trustStorePassword();

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password of the trust
     * store file
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    Optional<String> trustStorePasswordKey();

    /**
     * An optional parameter to trust only one specific certificate in the trust store (instead of trusting all certificates in
     * the store).
     */
    Optional<String> trustStoreCertAlias();
}
