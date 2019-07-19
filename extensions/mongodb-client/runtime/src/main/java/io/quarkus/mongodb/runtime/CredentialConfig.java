package io.quarkus.mongodb.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

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
     * Supported values: {@code MONGO-CR|GSSAPI|PLAIN|MONGODB-X509}
     */
    @ConfigItem
    public Optional<String> authMechanism;

    /**
     * Configures the source of the authentication credentials.
     * This is typically the database that the credentials have been created. The value defaults to the database
     * specified in the path portion of the connection string or in the 'database' configuration property..
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
}
