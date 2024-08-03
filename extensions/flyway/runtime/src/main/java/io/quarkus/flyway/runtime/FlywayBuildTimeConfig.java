package io.quarkus.flyway.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FlywayBuildTimeConfig {

    /**
     * Gets the {@link FlywayDataSourceBuildTimeConfig} for the given datasource name.
     */
    public FlywayDataSourceBuildTimeConfig getConfigForDataSourceName(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }
        return namedDataSources.getOrDefault(dataSourceName, FlywayDataSourceBuildTimeConfig.defaultConfig());
    }

    /**
     * Whether Flyway is enabled *during the build*.
     *
     * If Flyway is disabled, the Flyway beans won't be created and Flyway won't be usable.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Flyway configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayDataSourceBuildTimeConfig defaultDataSource;

    /**
     * Named datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    public Map<String, FlywayDataSourceBuildTimeConfig> namedDataSources = Collections.emptyMap();
}