package io.quarkus.reactive.h2.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveH2Config {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.h2")
    public DataSourceReactiveH2Config defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveH2OuterNamedConfig> namedDataSources;

    public DataSourceReactiveH2Config getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveH2OuterNamedConfig dataSourceReactiveH2OuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveH2OuterNamedConfig == null) {
            return new DataSourceReactiveH2Config();
        }

        return dataSourceReactiveH2OuterNamedConfig.reactive.h2;
    }

    @ConfigGroup
    public static class DataSourceReactiveH2OuterNamedConfig {

        /**
         * The H2-specific configuration.
         */
        public DataSourceReactiveH2OuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveH2OuterNestedNamedConfig {

        /**
         * The H2-specific configuration.
         */
        public DataSourceReactiveH2Config h2;
    }

}
