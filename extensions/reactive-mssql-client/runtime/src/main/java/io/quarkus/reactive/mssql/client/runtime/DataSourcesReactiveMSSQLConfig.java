package io.quarkus.reactive.mssql.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactiveMSSQLConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.mssql")
    public DataSourceReactiveMSSQLConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactiveMSSQLOuterNamedConfig> namedDataSources;

    public DataSourceReactiveMSSQLConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactiveMSSQLOuterNamedConfig dataSourceReactiveMSSQLOuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactiveMSSQLOuterNamedConfig == null) {
            return new DataSourceReactiveMSSQLConfig();
        }

        return dataSourceReactiveMSSQLOuterNamedConfig.reactive.mssql;
    }

    @ConfigGroup
    public static class DataSourceReactiveMSSQLOuterNamedConfig {

        /**
         * The MSSQL-specific configuration.
         */
        public DataSourceReactiveMSSQLOuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactiveMSSQLOuterNestedNamedConfig {

        /**
         * The MSSQL-specific configuration.
         */
        public DataSourceReactiveMSSQLConfig mssql;
    }

}
