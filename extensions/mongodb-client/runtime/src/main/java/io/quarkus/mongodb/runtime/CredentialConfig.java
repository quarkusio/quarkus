package io.quarkus.mongodb.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

/**
 * Configures the credentials and authentication mechanism to connect to the MongoDB server.
 */
@ConfigGroup
public class CredentialConfig {

    /**
     * Configures the username.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Configures the password.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Configures the authentication mechanism to use if a credential was supplied.
     * The default is unspecified, in which case the client will pick the most secure mechanism available based on the
     * sever version. For the GSSAPI and MONGODB-X509 mechanisms, no password is accepted, only the username.
     * Supported values: null or {@code GSSAPI|PLAIN|MONGODB-X509|SCRAM_SHA_1|SCRAM_SHA_256|MONGODB_AWS}
     */
    @ConfigItem
    public Optional<String> authMechanism;

    /**
     * Configures the source of the authentication credentials.
     * This is typically the database that the credentials have been created. The value defaults to the database
     * specified in the path portion of the connection string or in the 'database' configuration property.
     * If the database is specified in neither place, the default value is {@code admin}. This option is only
     * respected when using the MONGO-CR mechanism (the default).
     */
    @ConfigItem
    public Optional<String> authSource;

    /**
     * Allows passing authentication mechanism properties.
     */
    @ConfigItem
    public Map<String, String> authMechanismProperties;

    /**
     * The credentials provider name
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * <p>
     * For Vault it is: vault-credentials-provider. Not necessary if there is only one credentials provider available.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> credentialsProviderName = Optional.empty();
}
