package io.quarkus.reactive.datasource.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveRuntimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive")
    public DataSourceReactiveRuntimeConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveOuterNamedRuntimeConfig> namedDataSources;

    public DataSourceReactiveRuntimeConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveOuterNamedRuntimeConfig dataSourceReactiveOuterNamedRuntimeConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveOuterNamedRuntimeConfig == null) {
            return new DataSourceReactiveRuntimeConfig();
        }

        return dataSourceReactiveOuterNamedRuntimeConfig.reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveOuterNamedRuntimeConfig {

        /**
         * The JDBC runtime configuration.
         */
        @ConfigItem
        public DataSourceReactiveRuntimeConfig reactive;
    }
}
