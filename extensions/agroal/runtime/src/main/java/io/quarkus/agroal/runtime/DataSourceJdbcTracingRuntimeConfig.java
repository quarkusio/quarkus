package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceJdbcTracingRuntimeConfig {

    /**
     * Enable JDBC tracing.
     */
    @ConfigItem(defaultValueDocumentation = "false if quarkus.datasource.jdbc.tracing=false and true if quarkus.datasource.jdbc.tracing=true")
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Trace calls with active Spans only
     */
    @ConfigItem(defaultValue = "false")
    public boolean traceWithActiveSpanOnly = false;

    /**
     * Ignore specific queries from being traced
     */
    @ConfigItem(defaultValueDocumentation = "Ignore specific queries from being traced, multiple queries can be specified separated by semicolon, double quotes should be escaped with \\")
    public Optional<String> ignoreForTracing = Optional.empty();

}
