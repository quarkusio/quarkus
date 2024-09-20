package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * @deprecated in favor of OpenTelemetry {@link DataSourceJdbcRuntimeConfig#telemetry()}
 */
@ConfigGroup
@Deprecated(forRemoval = true, since = "3.16")
public interface DataSourceJdbcTracingRuntimeConfig {

    /**
     * Enable JDBC tracing.
     */
    @ConfigDocDefault("false if quarkus.datasource.jdbc.tracing=false and true if quarkus.datasource.jdbc.tracing=true")
    Optional<Boolean> enabled();

    /**
     * Trace calls with active Spans only
     */
    @WithDefault("false")
    boolean traceWithActiveSpanOnly();

    /**
     * Ignore specific queries from being traced
     */
    @ConfigDocDefault("Ignore specific queries from being traced, multiple queries can be specified separated by semicolon, double quotes should be escaped with \\")
    Optional<String> ignoreForTracing();

}
