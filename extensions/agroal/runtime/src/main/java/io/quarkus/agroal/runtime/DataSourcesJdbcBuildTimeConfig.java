package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DataSourcesJdbcBuildTimeConfig {

    /**
     * The default datasource.
     */
    DataSourceJdbcBuildTimeConfig jdbc();

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    Map<String, DataSourceJdbcOuterNamedBuildTimeConfig> namedDataSources();

    @ConfigGroup
    public interface DataSourceJdbcOuterNamedBuildTimeConfig {

        /**
         * The JDBC build time configuration.
         */
        DataSourceJdbcBuildTimeConfig jdbc();
    }

    default DataSourceJdbcBuildTimeConfig getDataSourceJdbcBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return jdbc();
        }

        DataSourceJdbcOuterNamedBuildTimeConfig dataSourceJdbcBuildTimeConfig = namedDataSources().get(dataSourceName);

        if (dataSourceJdbcBuildTimeConfig != null) {
            return dataSourceJdbcBuildTimeConfig.jdbc();
        }

        return ConfigUtils.getInitializedConfigGroup(DataSourceJdbcBuildTimeConfig.class, "quarkus.datasource.*.jdbc");
    }
}
