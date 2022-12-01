package io.quarkus.reactive.datasource.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DataSourcesReactiveBuildTimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive")
    public DataSourceReactiveBuildTimeConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveOuterNamedBuildTimeConfig> namedDataSources;

    public DataSourceReactiveBuildTimeConfig getDataSourceReactiveBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveOuterNamedBuildTimeConfig dataSourceReactiveOuterNamedBuildTimeConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveOuterNamedBuildTimeConfig == null) {
            return new DataSourceReactiveBuildTimeConfig();
        }

        return dataSourceReactiveOuterNamedBuildTimeConfig.reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveOuterNamedBuildTimeConfig {

        /**
         * The JDBC build time configuration.
         */
        @ConfigItem
        public DataSourceReactiveBuildTimeConfig reactive;
    }
}
