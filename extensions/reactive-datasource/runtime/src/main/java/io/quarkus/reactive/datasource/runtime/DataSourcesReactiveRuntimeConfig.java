package io.quarkus.reactive.datasource.runtime;

import java.util.HashMap;
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
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesReactiveRuntimeConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceReactiveOuterNamedRuntimeConfig> dataSources();

    /**
     * The default datasource.
     *
     * @deprecated Use {@code dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).reactive()} instead.
     */
    @Deprecated
    default DataSourceReactiveRuntimeConfig defaultDataSource() {
        return dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).reactive();
    }

    /**
     * Additional named datasources.
     *
     * @deprecated Use {@code dataSources()} instead -- this will include the default datasource.
     */
    @Deprecated
    default Map<String, DataSourceReactiveOuterNamedRuntimeConfig> namedDataSources() {
        Map<String, DataSourceReactiveOuterNamedRuntimeConfig> withoutDefault = new HashMap<>(dataSources());
        withoutDefault.remove(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
        return withoutDefault;
    }

    /**
     * Additional named datasources.
     *
     * @deprecated Use {@code dataSources().get(dataSourceName).reactive()} instead.
     */
    @Deprecated
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
