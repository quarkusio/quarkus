package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Optional;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceRuntimeConfig {

    /**
     * The datasource URL
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The datasource username
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The datasource password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The credentials provider name
     */
    @ConfigItem
    public Optional<String> credentialsProvider;

    /**
     * The credentials provider type.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * <p>
     * For Vault it is: vault-credentials-provider. Not necessary if there is only one credentials provider available.
     */
    @ConfigItem
    public Optional<String> credentialsProviderType;

    /**
     * The initial size of the pool. Usually you will want to set the initial size to match at least the
     * minimal size, but this is not enforced so to allow for architectures which prefer a lazy initialization
     * of the connections on boot, while being able to sustain a minimal pool size after boot.
     */
    @ConfigItem
    public Optional<Integer> initialSize;

    /**
     * The datasource pool minimum size
     */
    @ConfigItem(defaultValue = "0")
    public int minSize;

    /**
     * The datasource pool maximum size
     */
    @ConfigItem(defaultValue = "20")
    public int maxSize;

    /**
     * The interval at which we validate idle connections in the background.
     * <p>
     * Set to {@code 0} to disable background validation.
     */
    @ConfigItem(defaultValue = "2M")
    public Optional<Duration> backgroundValidationInterval;

    /**
     * The timeout before cancelling the acquisition of a new connection
     */
    @ConfigItem(defaultValue = "5")
    public Optional<Duration> acquisitionTimeout;

    /**
     * The interval at which we check for connection leaks.
     */
    @ConfigItem
    public Optional<Duration> leakDetectionInterval;

    /**
     * The interval at which we try to remove idle connections.
     */
    @ConfigItem(defaultValue = "5M")
    public Optional<Duration> idleRemovalInterval;

    /**
     * The max lifetime of a connection.
     */
    @ConfigItem
    public Optional<Duration> maxLifetime;

    /**
     * The transaction isolation level.
     */
    @ConfigItem
    public Optional<AgroalConnectionFactoryConfiguration.TransactionIsolation> transactionIsolationLevel;

    /**
     * When enabled Agroal will be able to produce a warning when a connection is returned
     * to the pool without the application having closed all open statements.
     * This is unrelated with tracking of open connections.
     * Disable for peak performance, but only when there's high confidence that
     * no leaks are happening.
     */
    @ConfigItem(defaultValue = "true")
    public boolean detectStatementLeaks;

    /**
     * Query executed when first using a connection.
     */
    @ConfigItem
    public Optional<String> newConnectionSql;

    /**
     * Query executed to validate a connection.
     */
    @ConfigItem
    public Optional<String> validationQuerySql;
}
