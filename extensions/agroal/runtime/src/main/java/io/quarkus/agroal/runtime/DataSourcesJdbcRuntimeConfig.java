package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourcesJdbcRuntimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem
    public DataSourceJdbcRuntimeConfig jdbc;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceJdbcOuterNamedRuntimeConfig> namedDataSources;

    @ConfigGroup
    public static class DataSourceJdbcOuterNamedRuntimeConfig {

        /**
         * The JDBC runtime configuration.
         */
        @ConfigItem
        public DataSourceJdbcRuntimeConfig jdbc;
    }
}
