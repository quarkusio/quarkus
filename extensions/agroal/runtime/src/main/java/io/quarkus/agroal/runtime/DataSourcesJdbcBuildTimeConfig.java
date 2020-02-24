package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DataSourcesJdbcBuildTimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem
    public DataSourceJdbcBuildTimeConfig jdbc;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceJdbcOuterNamedBuildTimeConfig> namedDataSources;

    @ConfigGroup
    public static class DataSourceJdbcOuterNamedBuildTimeConfig {

        /**
         * The JDBC build time configuration.
         */
        @ConfigItem
        public DataSourceJdbcBuildTimeConfig jdbc;
    }

}
