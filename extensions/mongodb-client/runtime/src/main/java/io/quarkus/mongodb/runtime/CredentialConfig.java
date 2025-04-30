package io.quarkus.mongodb.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * Configures the credentials and authentication mechanism to connect to the MongoDB server.
 */
@ConfigGroup
public interface CredentialConfig {

    /**
     * Configures the username.
     */
    Optional<String> username();

    /**
     * Configures the password.
     */
    Optional<String> password();

    /**
     * Configures the authentication mechanism to use if a credential was supplied.
     * The default is unspecified, in which case the client will pick the most secure mechanism available based on the
     * sever version. For the GSSAPI and MONGODB-X509 mechanisms, no password is accepted, only the username.
     * Supported values: null or {@code GSSAPI|PLAIN|MONGODB-X509|SCRAM_SHA_1|SCRAM_SHA_256|MONGODB_AWS}
     */
    Optional<String> authMechanism();

    /**
     * Configures the source of the authentication credentials.
     * This is typically the database where the credentials have been created.
     * The value defaults to the database
     * specified in the path portion of the connection string or in the 'database' configuration property.
     * If the database is specified in neither place, the default value is {@code admin}. This option is only
     * respected when using the MONGO-CR mechanism (the default).
     */
    Optional<String> authSource();

    /**
     * Allows passing authentication mechanism properties.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> authMechanismProperties();

    /**
     * The credentials provider name
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
}
