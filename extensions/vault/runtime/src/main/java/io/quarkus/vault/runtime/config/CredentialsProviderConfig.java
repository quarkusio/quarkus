package io.quarkus.vault.runtime.config;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CredentialsProviderConfig {

    public static final String DATABASE_MOUNT = "database";
    public static final String DEFAULT_REQUEST_PATH = "creds";

    /**
     * Database credentials role, as defined by https://www.vaultproject.io/docs/secrets/databases/index.html
     *
     * Only one of `database-credentials-role`, `credentials-role` or `kv-path` can be defined.
     *
     * @deprecated Use `credentials-role` with `credentials-mount` set to `database`
     * @asciidoclet
     */
    @Deprecated(since = "2.6")
    @ConfigItem
    public Optional<String> databaseCredentialsRole;

    /**
     * Dynamic credentials' role.
     *
     * Roles are defined by the secret engine in use. For example, `database` credentials roles are defined
     * by the database secrets engine described at https://www.vaultproject.io/docs/secrets/databases/index.html.
     *
     * One of `credentials-role` or `kv-path` can to be defined. not both.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> credentialsRole;

    /**
     * Mount of dynamic credentials secrets engine, for example `database` or `rabbitmq`.
     *
     * Only used when `credentials-role` is defined.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = DATABASE_MOUNT)
    public String credentialsMount;

    /**
     * Path of dynamic credentials request.
     *
     * Request paths are dictated by the secret engine in use. For standard secret engines this should be
     * left as the default of `creds`.
     *
     * Only used when `credentials-role` is defined.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = DEFAULT_REQUEST_PATH)
    public String credentialsRequestPath;

    /**
     * A path in vault kv store, where we will find the kv-key.
     *
     * One of `database-credentials-role` or `kv-path` needs to be defined. not both.
     *
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> kvPath;

    /**
     * Key name to search in vault path `kv-path`. The value for that key is the credential.
     *
     * `kv-key` should not be defined if `kv-path` is not.
     *
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = PASSWORD_PROPERTY_NAME)
    public String kvKey;

    @Override
    public String toString() {
        return "CredentialsProviderConfig{" +
                "databaseCredentialsRole='" + databaseCredentialsRole.get() + '\'' +
                ", credentialsRole='" + credentialsRole.get() + '\'' +
                ", credentialsMount='" + credentialsMount + '\'' +
                ", credentialsRequestPath='" + credentialsRequestPath + '\'' +
                ", kvPath='" + kvPath.get() + '\'' +
                ", kvKey='" + kvKey + '\'' +
                '}';
    }
}
