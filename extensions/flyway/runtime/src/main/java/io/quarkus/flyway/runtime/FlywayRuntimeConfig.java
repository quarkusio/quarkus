package io.quarkus.flyway.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.RUN_TIME)
public final class FlywayRuntimeConfig {
    /*
     * Creates a {@link FlywayMultiDatasourceRuntimeConfig} with default settings.
     * 
     * @return {@link FlywayMultiDatasourceRuntimeConfig}
     */
    public static final FlywayRuntimeConfig defaultConfig() {
        return new FlywayRuntimeConfig();
    }

    /**
     * Gets the {@link FlywayDataSourceRuntimeConfig} for the given datasource name.<br>
     * The name of the default datasource is an empty {@link String}.
     * 
     * @param dataSourceName {@link String}
     * @return {@link FlywayDataSourceRuntimeConfig}
     * @throws NullPointerException if dataSourceName is null.
     */
    public FlywayDataSourceRuntimeConfig getConfigForDataSourceName(String dataSourceName) {
        return namedDataSources.getOrDefault(dataSourceName, FlywayDataSourceRuntimeConfig.defaultConfig());
    }

    /**
     * Flyway configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayDataSourceRuntimeConfig defaultDataSource = FlywayDataSourceRuntimeConfig.defaultConfig();

    /**
     * Flyway configurations for named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, FlywayDataSourceRuntimeConfig> namedDataSources = Collections.emptyMap();
}