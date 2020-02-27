package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * This configuration class is here for compatibility reason and is planned for removal.
 */
@Deprecated
@ConfigGroup
public class LegacyDataSourceJdbcBuildTimeConfig {

    /**
     * @deprecated use quarkus.datasource.db-kind (and quarkus.datasource.jdbc.driver if you really need a specific JDBC
     *             driver).
     */
    @ConfigItem
    @Deprecated
    public Optional<String> driver;

    /**
     * @deprecated use quarkus.datasource.jdbc.transactions instead.
     */
    @ConfigItem(defaultValue = "enabled")
    @Deprecated
    public TransactionIntegration transactions = TransactionIntegration.ENABLED;

    /**
     * @deprecated use quarkus.datasource.jdbc.enable-metrics instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> enableMetrics = Optional.empty();
}
