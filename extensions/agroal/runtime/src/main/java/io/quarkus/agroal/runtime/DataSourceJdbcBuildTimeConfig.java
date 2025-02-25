package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface DataSourceJdbcBuildTimeConfig {

    /**
     * If we create a JDBC datasource for this datasource.
     */
    @WithParentName
    @WithDefault("true")
    boolean enabled();

    /**
     * The datasource driver class name
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> driver();

    /**
     * Whether we want to use regular JDBC transactions, XA, or disable all transactional capabilities.
     * <p>
     * When enabling XA you will need a driver implementing {@link javax.sql.XADataSource}.
     */
    @WithDefault("enabled")
    TransactionIntegration transactions();

    /**
     * Enable datasource metrics collection. If unspecified, collecting metrics will be enabled by default if
     * a metrics extension is active.
     */
    Optional<Boolean> enableMetrics();

    /**
     * Enable OpenTelemetry JDBC instrumentation.
     */
    @WithDefault("false")
    boolean telemetry();
}
