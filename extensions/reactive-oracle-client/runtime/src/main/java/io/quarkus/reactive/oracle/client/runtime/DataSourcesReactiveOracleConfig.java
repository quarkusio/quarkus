package io.quarkus.reactive.oracle.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveOracleConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.oracle")
    public DataSourceReactiveOracleConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveOracleOuterNamedConfig> namedDataSources;

    public DataSourceReactiveOracleConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveOracleOuterNamedConfig dataSourceReactiveOracleOuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveOracleOuterNamedConfig == null) {
            return new DataSourceReactiveOracleConfig();
        }

        return dataSourceReactiveOracleOuterNamedConfig.reactive.oracle;
    }

    @ConfigGroup
    public static class DataSourceReactiveOracleOuterNamedConfig {

        /**
         * The Oracle-specific configuration.
         */
        public DataSourceReactiveOracleOuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveOracleOuterNestedNamedConfig {

        /**
         * The Oracle-specific configuration.
         */
        public DataSourceReactiveOracleConfig oracle;
    }

}
