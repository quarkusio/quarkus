package io.quarkus.liquibase.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Liquibase runtime configuration.
 */
@ConfigRoot(name = "liquibase", phase = ConfigPhase.RUN_TIME)
public final class LiquibaseRuntimeConfig {

    /**
     * Gets the default runtime configuration
     * 
     * @return the liquibase runtime default configuration
     */
    public static LiquibaseRuntimeConfig defaultConfig() {
        return new LiquibaseRuntimeConfig();
    }

    /**
     * Gets the {@link LiquibaseDataSourceRuntimeConfig} for the given datasource name.
     */
    public LiquibaseDataSourceRuntimeConfig getConfigForDataSourceName(String dataSourceName) {
        return namedDataSources.getOrDefault(dataSourceName, LiquibaseDataSourceRuntimeConfig.defaultConfig());
    }

    /**
     * Liquibase configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LiquibaseDataSourceRuntimeConfig defaultDataSource = LiquibaseDataSourceRuntimeConfig.defaultConfig();

    /**
     * Liquibase configurations for named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, LiquibaseDataSourceRuntimeConfig> namedDataSources = Collections.emptyMap();
}