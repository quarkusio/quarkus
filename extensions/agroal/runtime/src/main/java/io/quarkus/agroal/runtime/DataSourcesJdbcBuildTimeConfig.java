package io.quarkus.agroal.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
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

    /**
     * Dev UI.
     */
    @WithDefaults
    @WithName("dev-ui")
    DevUIBuildTimeConfig devui();

    @ConfigGroup
    public interface DataSourceJdbcOuterNamedBuildTimeConfig {

        /**
         * The JDBC build time configuration.
         */
        DataSourceJdbcBuildTimeConfig jdbc();
    }

    @ConfigGroup
    public interface DevUIBuildTimeConfig {

        /**
         * Activate or disable the dev ui page.
         */
        @WithDefault("true")
        public boolean enabled();

        /**
         * Allow sql queries in the Dev UI page
         */
        @WithDefault("false")
        public boolean allowSql();

        /**
         * Append this to the select done to fetch the table values. eg: LIMIT 100 or TOP 100
         */
        public Optional<String> appendToDefaultSelect();

        /**
         * Allowed database host. By default, only localhost is allowed. Any provided host here will also be allowed.
         * You can use the special value {@code *} to allow any DB host.
         */
        public Optional<String> allowedDBHost();
    }
}
