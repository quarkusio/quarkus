package io.quarkus.reactive.datasource.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DataSourcesReactiveBuildTimeConfig {

    /**
     * The default datasource.
     */
    @WithName("reactive")
    DataSourceReactiveBuildTimeConfig defaultDataSource();

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    Map<String, DataSourceReactiveOuterNamedBuildTimeConfig> namedDataSources();

    default DataSourceReactiveBuildTimeConfig getDataSourceReactiveBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource();
        }

        DataSourceReactiveOuterNamedBuildTimeConfig dataSourceReactiveBuildTimeConfig = namedDataSources().get(dataSourceName);

        if (dataSourceReactiveBuildTimeConfig != null) {
            return dataSourceReactiveBuildTimeConfig.reactive();
        }

        return ConfigUtils.getInitializedConfigGroup(DataSourceReactiveBuildTimeConfig.class, "quarkus.datasource.*.reactive");
    }

    @ConfigGroup
    public interface DataSourceReactiveOuterNamedBuildTimeConfig {

        /**
         * The Reactive build time configuration.
         */
        public DataSourceReactiveBuildTimeConfig reactive();
    }
}
