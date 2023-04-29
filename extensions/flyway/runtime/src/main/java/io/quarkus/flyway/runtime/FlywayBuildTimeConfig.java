package io.quarkus.flyway.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.flyway")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FlywayBuildTimeConfig {

    /**
     * Gets the {@link FlywayDataSourceBuildTimeConfig} for the given datasource name.
     */
    default FlywayDataSourceBuildTimeConfig getConfigForDataSourceName(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource();
        }
        FlywayDataSourceBuildTimeConfig config = namedDataSources().get(dataSourceName);
        if (config == null) {
            config = defaultDataSource();
        }
        return config;
    }

    /**
     * Flyway configuration for the default datasource.
     */
    @WithParentName
    FlywayDataSourceBuildTimeConfig defaultDataSource();

    /**
     * Flyway configurations for named datasources.
     */
    @WithParentName
    Map<String, FlywayDataSourceBuildTimeConfig> namedDataSources();
}
