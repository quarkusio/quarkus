package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceJdbcRuntimeConfig {

    /**
     * The datasource URL
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The initial size of the pool. Usually you will want to set the initial size to match at least the
     * minimal size, but this is not enforced so to allow for architectures which prefer a lazy initialization
     * of the connections on boot, while being able to sustain a minimal pool size after boot.
     */
    @ConfigItem
    public OptionalInt initialSize;

    /**
     * The datasource pool minimum size
     */
    @ConfigItem
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
     * Perform foreground validation on connections that have been idle for longer than the specified interval.
     */
    @ConfigItem
    public Optional<Duration> foregroundValidationInterval;

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
     * Collect and display extra troubleshooting info on leaked connections.
     */
    @ConfigItem
    public boolean extendedLeakReport;

    /**
     * Allows connections to be flushed upon return to the pool. It's not enabled by default.
     */
    @ConfigItem
    public boolean flushOnClose;

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

    /**
     * Disable pooling to prevent reuse of Connections. Use this with when an external pool manages the life-cycle
     * of Connections.
     */
    @ConfigItem(defaultValue = "true")
    public boolean poolingEnabled;

    /**
     * Require an active transaction when acquiring a connection. Recommended for production.
     * WARNING: Some extensions acquire connections without holding a transaction for things like schema updates and schema
     * validation. Setting this setting to STRICT may lead to failures in those cases.
     */
    @ConfigItem
    public Optional<AgroalConnectionPoolConfiguration.TransactionRequirement> transactionRequirement;

    /**
     * Other unspecified properties to be passed to the JDBC driver when creating new connections.
     */
    @ConfigItem
    public Map<String, String> additionalJdbcProperties;

    /**
     * Enable JDBC tracing.
     */
    @ConfigItem
    public DataSourceJdbcTracingRuntimeConfig tracing;

}
