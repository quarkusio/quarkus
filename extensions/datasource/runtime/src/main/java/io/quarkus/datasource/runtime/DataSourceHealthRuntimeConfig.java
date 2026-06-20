package io.quarkus.datasource.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceHealthRuntimeConfig {

    /**
     * Minimum interval between health check executions.
     * <p>
     * Health check results are cached for this duration to avoid performing a JDBC validation
     * query on every probe request. Set to {@code 0} to disable caching and execute the
     * validation query on every health check call.
     */
    @WithDefault("10s")
    Duration ttl();
}
