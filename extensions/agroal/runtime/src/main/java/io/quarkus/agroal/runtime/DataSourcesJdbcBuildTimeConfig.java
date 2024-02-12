package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DataSourcesJdbcBuildTimeConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceJdbcOuterNamedBuildTimeConfig> dataSources();

    @ConfigGroup
    public interface DataSourceJdbcOuterNamedBuildTimeConfig {

        /**
         * The JDBC build time configuration.
         */
        DataSourceJdbcBuildTimeConfig jdbc();
    }
}
