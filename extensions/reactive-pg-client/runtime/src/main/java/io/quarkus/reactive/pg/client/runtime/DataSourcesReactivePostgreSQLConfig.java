package io.quarkus.reactive.pg.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesReactivePostgreSQLConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = "reactive.postgresql")
    public DataSourceReactivePostgreSQLConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceReactivePostgreSQLOuterNamedConfig> namedDataSources;

    public DataSourceReactivePostgreSQLConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceReactivePostgreSQLOuterNamedConfig dataSourceReactivePostgreSQLOuterNamedConfig = namedDataSources
                .get(dataSourceName);
        if (dataSourceReactivePostgreSQLOuterNamedConfig == null) {
            return new DataSourceReactivePostgreSQLConfig();
        }

        return dataSourceReactivePostgreSQLOuterNamedConfig.reactive.postgresql;
    }

    @ConfigGroup
    public static class DataSourceReactivePostgreSQLOuterNamedConfig {

        /**
         * The PostgreSQL-specific configuration.
         */
        public DataSourceReactivePostgreSQLOuterNestedNamedConfig reactive;
    }

    @ConfigGroup
    public static class DataSourceReactivePostgreSQLOuterNestedNamedConfig {

        /**
         * The PostgreSQL-specific configuration.
         */
        public DataSourceReactivePostgreSQLConfig postgresql;
    }

}
