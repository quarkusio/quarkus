package io.quarkus.reactive.db2.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveDB2Config {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.db2")
    public DataSourceReactiveDB2Config defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveDB2OuterNamedConfig> namedDataSources;

    public DataSourceReactiveDB2Config getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveDB2OuterNamedConfig dataSourceReactiveDB2OuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveDB2OuterNamedConfig == null) {
            return new DataSourceReactiveDB2Config();
        }

        return dataSourceReactiveDB2OuterNamedConfig.reactive.db2;
    }

    @ConfigGroup
    public static class DataSourceReactiveDB2OuterNamedConfig {

        /**
         * The DB2-specific configuration.
         */
        public DataSourceReactiveDB2OuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveDB2OuterNestedNamedConfig {

        /**
         * The DB2-specific configuration.
         */
        public DataSourceReactiveDB2Config db2;
    }

}
