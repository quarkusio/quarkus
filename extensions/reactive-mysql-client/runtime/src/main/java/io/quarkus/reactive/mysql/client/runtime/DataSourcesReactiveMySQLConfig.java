package io.quarkus.reactive.mysql.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveMySQLConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.mysql")
    public DataSourceReactiveMySQLConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveMySQLOuterNamedConfig> namedDataSources;

    public DataSourceReactiveMySQLConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveMySQLOuterNamedConfig dataSourceReactiveMySQLOuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveMySQLOuterNamedConfig == null) {
            return new DataSourceReactiveMySQLConfig();
        }

        return dataSourceReactiveMySQLOuterNamedConfig.reactive.mysql;
    }

    @ConfigGroup
    public static class DataSourceReactiveMySQLOuterNamedConfig {

        /**
         * The MySQL-specific configuration.
         */
        public DataSourceReactiveMySQLOuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveMySQLOuterNestedNamedConfig {

        /**
         * The MySQL-specific configuration.
         */
        public DataSourceReactiveMySQLConfig mysql;
    }

}
