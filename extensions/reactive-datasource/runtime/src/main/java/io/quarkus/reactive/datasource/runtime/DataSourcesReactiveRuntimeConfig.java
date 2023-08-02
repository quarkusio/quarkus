package io.quarkus.reactive.datasource.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesReactiveRuntimeConfig {

    /**
     * The default datasource.
     */
    @WithName("reactive")
    DataSourceReactiveRuntimeConfig defaultDataSource();

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    Map<String, DataSourceReactiveOuterNamedRuntimeConfig> namedDataSources();

    default DataSourceReactiveRuntimeConfig getDataSourceReactiveRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource();
        }

        return namedDataSources().get(dataSourceName).reactive();
    }

    @ConfigGroup
    public interface DataSourceReactiveOuterNamedRuntimeConfig {

        /**
         * The JDBC runtime configuration.
         */
        public DataSourceReactiveRuntimeConfig reactive();
    }
}
