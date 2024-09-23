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
 * The liquibase build time configuration
 */
@ConfigRoot(name = "liquibase", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class LiquibaseBuildTimeConfig {

    /**
     * Gets the default build time configuration
     *
     * @return the liquibase build time default configuration
     */
    public static LiquibaseBuildTimeConfig defaultConfig() {
        LiquibaseBuildTimeConfig defaultConfig = new LiquibaseBuildTimeConfig();
        defaultConfig.defaultDataSource = LiquibaseDataSourceBuildTimeConfig.defaultConfig();
        return defaultConfig;
    }

    /**
     * Gets the {@link LiquibaseBuildTimeConfig} for the given datasource name.
     */
    public LiquibaseDataSourceBuildTimeConfig getConfigForDataSourceName(String dataSourceName) {
        return DataSourceUtil.isDefault(dataSourceName)
                ? defaultDataSource
                : namedDataSources.getOrDefault(dataSourceName, LiquibaseDataSourceBuildTimeConfig.defaultConfig());
    }

    /**
     * Liquibase configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LiquibaseDataSourceBuildTimeConfig defaultDataSource;

    /**
     * Named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    public Map<String, LiquibaseDataSourceBuildTimeConfig> namedDataSources = Collections.emptyMap();
}
