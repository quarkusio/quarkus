package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Optional;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * This configuration class is here for compatibility reason and is planned for removal.
 */
@Deprecated
@ConfigGroup
public class LegacyDataSourceJdbcRuntimeConfig {

    /**
     * @deprecated use quarkus.datasource.jdbc.initial-size instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Integer> initialSize = Optional.empty();

    /**
     * @deprecated use quarkus.datasource.jdbc.min-size instead.
     */
    @ConfigItem(defaultValue = "0")
    @Deprecated
    public int minSize = 0;

    /**
     * @deprecated use quarkus.datasource.jdbc.background-validation-interval instead.
     */
    @ConfigItem(defaultValue = "2M")
    @Deprecated
    public Optional<Duration> backgroundValidationInterval = Optional.of(Duration.ofMinutes(2));

    /**
     * @deprecated use quarkus.datasource.jdbc.acquisition-timeout instead.
     */
    @ConfigItem(defaultValue = "5")
    @Deprecated
    public Optional<Duration> acquisitionTimeout = Optional.of(Duration.ofSeconds(5));

    /**
     * @deprecated use quarkus.datasource.jdbc.leak-detection-interval instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Duration> leakDetectionInterval = Optional.empty();

    /**
     * @deprecated use quarkus.datasource.jdbc.idle-removal-interval instead.
     */
    @ConfigItem(defaultValue = "5M")
    @Deprecated
    public Optional<Duration> idleRemovalInterval = Optional.of(Duration.ofMinutes(5));

    /**
     * @deprecated use quarkus.datasource.jdbc.max-lifetime instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Duration> maxLifetime = Optional.empty();

    /**
     * @deprecated use quarkus.datasource.jdbc.transaction-isolation-level instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<AgroalConnectionFactoryConfiguration.TransactionIsolation> transactionIsolationLevel = Optional.empty();

    /**
     * @deprecated use quarkus.datasource.jdbc.detect-statement-leaks instead.
     */
    @ConfigItem(defaultValue = "true")
    @Deprecated
    public boolean detectStatementLeaks = true;

    /**
     * @deprecated use quarkus.datasource.jdbc.new-connection-sql instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> newConnectionSql = Optional.empty();

    /**
     * @deprecated use quarkus.datasource.jdbc.validation-query-sql instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> validationQuerySql = Optional.empty();
}
