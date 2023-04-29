package io.quarkus.flyway.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.flyway")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FlywayRuntimeConfig {

    /**
     * Gets the {@link FlywayDataSourceRuntimeConfig} for the given datasource name.
     */
    default FlywayDataSourceRuntimeConfig getConfigForDataSourceName(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource();
        }
        FlywayDataSourceRuntimeConfig config = namedDataSources().get(dataSourceName);
        if (config == null) {
            config = defaultDataSource();
        }
        return config;
    }

    /**
     * Flag to enable / disable Flyway.
     *
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Flyway configuration for the default datasource.
     */
    @WithParentName
    FlywayDataSourceRuntimeConfig defaultDataSource();

    /**
     * Flyway configurations for named datasources.
     */
    @WithParentName
    Map<String, FlywayDataSourceRuntimeConfig> namedDataSources();
}
