package io.quarkus.flyway.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FlywayBuildTimeConfig {

    public static final FlywayBuildTimeConfig defaultConfig() {
        FlywayBuildTimeConfig defaultConfig = new FlywayBuildTimeConfig();
        defaultConfig.defaultDataSource = FlywayDataSourceBuildTimeConfig.defaultConfig();
        return defaultConfig;
    }

    /**
     * Gets the {@link FlywayDataSourceBuildTimeConfig} for the given datasource name.
     */
    public FlywayDataSourceBuildTimeConfig getConfigForDataSourceName(String dataSourceName) {
        return namedDataSources.getOrDefault(dataSourceName, FlywayDataSourceBuildTimeConfig.defaultConfig());
    }

    /**
     * Flyway configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayDataSourceBuildTimeConfig defaultDataSource;

    /**
     * Flyway configurations for named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, FlywayDataSourceBuildTimeConfig> namedDataSources = Collections.emptyMap();
}