package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

/**
 * A certificate configuration. Either the certificate and key files must be given, or a key store must be given.
 */
@ConfigGroup
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CertificateConfig {

    /**
     * The {@linkplain CredentialsProvider}.
     * If this property is configured then a matching 'CredentialsProvider' will be used
     * to get the keystore, keystore key and truststore passwords unless these passwords have already been configured.
     *
     * Please note that using MicroProfile {@linkplain ConfigSource} which is directly supported by Quarkus Configuration
     * should be preferred unless using `CredentialsProvider` provides for some additional security and dynamism.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * It is recommended to set this property even if there is only one credentials provider currently available
     * to ensure the same provider is always found in deployments where more than one provider may be available.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProviderName = Optional.empty();

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
     * A parameter to specify the password of the key store file. If not given, and if it can not be retrieved from
     * {@linkplain CredentialsProvider}, then the default ("password") is used.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem(defaultValueDocumentation = "password")
    public Optional<String> keyStorePassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password of the key
     * store file
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> keyStorePasswordKey;

    /**
     * An optional parameter to select a specific key in the key store. When SNI is disabled, if the key store contains multiple
     * keys and no alias is specified, the behavior is undefined.
     */
    @ConfigItem
    public Optional<String> keyStoreKeyAlias;

    /**
     * An optional parameter to define the password for the key, in case it's different from {@link #keyStorePassword}
     * If not given then it may be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    @ConfigItem
    public Optional<String> keyStoreKeyPassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password for the key
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> keyStoreKeyPasswordKey;

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
     * If not given then it may be retrieved from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * A parameter to specify a {@linkplain CredentialsProvider} property key which can be used to get the password of the trust
     * store file
     * from {@linkplain CredentialsProvider}.
     *
     * @see {@link #credentialsProvider}
     */
    @ConfigItem
    public Optional<String> trustStorePasswordKey;

    /**
     * An optional parameter to trust only one specific certificate in the trust store (instead of trusting all certificates in
     * the store).
     */
    @ConfigItem
    public Optional<String> trustStoreCertAlias;
}
