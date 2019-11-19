package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceBuildTimeConfig {

    /**
     * The datasource driver class name
     */
    @ConfigItem
    public Optional<String> driver;

    /**
     * Whether we want to use regular JDBC transactions, XA, or disable all transactional capabilities.
     * <p>
     * When enabling XA you will need a driver implementing {@link javax.sql.XADataSource}.
     */
    @ConfigItem(defaultValue = "enabled")
    public TransactionIntegration transactions;

    /**
     * Enable datasource metrics collection. If unspecified, collecting metrics will be enabled by default if the
     * smallrye-metrics extension is active.
     */
    @ConfigItem
    public Optional<Boolean> enableMetrics;

}
