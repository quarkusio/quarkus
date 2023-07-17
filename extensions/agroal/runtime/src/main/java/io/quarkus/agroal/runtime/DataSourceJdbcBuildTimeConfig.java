package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class DataSourceJdbcBuildTimeConfig {

    /**
     * If we create a JDBC datasource for this datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "true")
    public boolean enabled = true;

    /**
     * The datasource driver class name
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> driver = Optional.empty();

    /**
     * Whether we want to use regular JDBC transactions, XA, or disable all transactional capabilities.
     * <p>
     * When enabling XA you will need a driver implementing {@link javax.sql.XADataSource}.
     */
    @ConfigItem(defaultValue = "enabled")
    public TransactionIntegration transactions = TransactionIntegration.ENABLED;

    /**
     * Enable datasource metrics collection. If unspecified, collecting metrics will be enabled by default if
     * a metrics extension is active.
     */
    @ConfigItem
    public Optional<Boolean> enableMetrics = Optional.empty();

    /**
     * Enable JDBC tracing. Disabled by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean tracing = false;

    /**
     * Enable OpenTelemetry JDBC instrumentation.
     */
    @ConfigItem(defaultValue = "false")
    public boolean telemetry = false;
}
