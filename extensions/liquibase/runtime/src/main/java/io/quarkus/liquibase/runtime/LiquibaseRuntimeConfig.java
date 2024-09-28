package io.quarkus.liquibase.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
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
        return DataSourceUtil.isDefault(dataSourceName)
                ? defaultDataSource
                : namedDataSources.getOrDefault(dataSourceName, LiquibaseDataSourceRuntimeConfig.defaultConfig());
    }

    /**
     * Flag to enable / disable Liquibase.
     *
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Liquibase configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LiquibaseDataSourceRuntimeConfig defaultDataSource = LiquibaseDataSourceRuntimeConfig.defaultConfig();

    /**
     * Named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    public Map<String, LiquibaseDataSourceRuntimeConfig> namedDataSources = Collections.emptyMap();
}
