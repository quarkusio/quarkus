package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceJdbcTelemetryConfig {

    /**
     * Enable OpenTelemetry JDBC instrumentation.
     */
    @ConfigDocDefault("false if quarkus.datasource.jdbc.telemetry=false and true if quarkus.datasource.jdbc.telemetry=true")
    Optional<Boolean> enabled();

    /**
     * Enable tracing of the connection acquisition from the datasource.
     * When enabled, a span is created for each {@code getConnection()} call.
     */
    @WithDefault("false")
    boolean traceConnection();
}