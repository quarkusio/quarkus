package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface DataSourceJdbcRuntimeConfig {

    /**
     * The datasource URL
     */
    Optional<String> url();

    /**
     * The initial size of the pool. Usually you will want to set the initial size to match at least the
     * minimal size, but this is not enforced so to allow for architectures which prefer a lazy initialization
     * of the connections on boot, while being able to sustain a minimal pool size after boot.
     */
    OptionalInt initialSize();

    /**
     * The datasource pool minimum size
     */
    @WithDefault("0")
    int minSize();

    /**
     * The datasource pool maximum size
     */
    @WithDefault("20")
    int maxSize();

    /**
     * The interval at which we validate idle connections in the background.
     * <p>
     * Set to {@code 0} to disable background validation.
     */
    @WithDefault("2M")
    Duration backgroundValidationInterval();

    /**
     * Perform foreground validation on connections that have been idle for longer than the specified interval.
     */
    Optional<Duration> foregroundValidationInterval();

    /**
     * The timeout before cancelling the acquisition of a new connection
     */
    @WithDefault("5S")
    Optional<Duration> acquisitionTimeout();

    /**
     * The interval at which we check for connection leaks.
     */

    @ConfigDocDefault("This feature is disabled by default.")
    Optional<Duration> leakDetectionInterval();

    /**
     * The interval at which we try to remove idle connections.
     */
    @WithDefault("5M")
    Duration idleRemovalInterval();

    /**
     * The max lifetime of a connection.
     */
    @ConfigDocDefault("By default, there is no restriction on the lifespan of a connection.")
    Optional<Duration> maxLifetime();

    /**
     * The transaction isolation level.
     */
    Optional<AgroalConnectionFactoryConfiguration.TransactionIsolation> transactionIsolationLevel();

    /**
     * Collect and display extra troubleshooting info on leaked connections.
     */
    @WithDefault("false")
    boolean extendedLeakReport();

    /**
     * Allows connections to be flushed upon return to the pool. It's not enabled by default.
     */
    @WithDefault("false")
    boolean flushOnClose();

    /**
     * When enabled, Agroal will be able to produce a warning when a connection is returned
     * to the pool without the application having closed all open statements.
     * This is unrelated with tracking of open connections.
     * Disable for peak performance, but only when there's high confidence that
     * no leaks are happening.
     */
    @WithDefault("true")
    boolean detectStatementLeaks();

    /**
     * Query executed when first using a connection.
     */
    Optional<String> newConnectionSql();

    /**
     * Query executed to validate a connection.
     */
    Optional<String> validationQuerySql();

    /**
     * Forces connection validation prior to acquisition (foreground validation) regardless of the idle status.
     * <p>
     * Because of the overhead of performing validation on every call, it’s recommended to rely on default idle validation
     * instead, and to leave this to `false`.
     */
    @WithDefault("false")
    boolean validateOnBorrow();

    /**
     * Disable pooling to prevent reuse of Connections. Use this when an external pool manages the life-cycle
     * of Connections.
     */
    @WithDefault("true")
    boolean poolingEnabled();

    /**
     * Require an active transaction when acquiring a connection. Recommended for production.
     * WARNING: Some extensions acquire connections without holding a transaction for things like schema updates and schema
     * validation. Setting this setting to STRICT may lead to failures in those cases.
     */
    Optional<AgroalConnectionPoolConfiguration.TransactionRequirement> transactionRequirement();

    /**
     * Other unspecified properties to be passed to the JDBC driver when creating new connections.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> additionalJdbcProperties();

    /**
     * Enable JDBC tracing.
     *
     * @deprecated in favor of OpenTelemetry {@link #telemetry()}
     */
    @Deprecated(forRemoval = true, since = "3.16")
    DataSourceJdbcTracingRuntimeConfig tracing();

    /**
     * Enable OpenTelemetry JDBC instrumentation.
     */
    @WithName("telemetry.enabled")
    @ConfigDocDefault("false if quarkus.datasource.jdbc.telemetry=false and true if quarkus.datasource.jdbc.telemetry=true")
    Optional<Boolean> telemetry();

}
