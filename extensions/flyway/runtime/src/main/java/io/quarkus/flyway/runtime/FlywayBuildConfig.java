package io.quarkus.flyway.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FlywayBuildConfig {
    /*
     * Creates a {@link FlywayMultiDatasourceBuildConfig} with default settings.
     * 
     * @return {@link FlywayMultiDatasourceBuildConfig}
     */
    public static final FlywayBuildConfig defaultConfig() {
        return new FlywayBuildConfig();
    }

    /**
     * Gets the {@link FlywayDataSourceBuildConfig} for the given datasource name.<br>
     * The name of the default datasource is an empty {@link String}.
     * 
     * @param dataSourceName {@link String}
     * @return {@link FlywayDataSourceBuildConfig}
     * @throws NullPointerException if dataSourceName is null.
     */
    public FlywayDataSourceBuildConfig getConfigForDataSourceName(String dataSourceName) {
        return namedDataSources.getOrDefault(dataSourceName, FlywayDataSourceBuildConfig.defaultConfig());
    }

    /**
     * Flyway configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayDataSourceBuildConfig defaultDataSource = FlywayDataSourceBuildConfig.defaultConfig();

    /**
     * Flyway configurations for named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, FlywayDataSourceBuildConfig> namedDataSources = Collections.emptyMap();
}