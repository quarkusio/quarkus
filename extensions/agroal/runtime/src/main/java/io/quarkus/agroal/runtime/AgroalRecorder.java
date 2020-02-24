package io.quarkus.agroal.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalRecorder {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public void configureDatasources(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            boolean disableSslSupport) {
        Arc.container().instance(AbstractDataSourceProducer.class).get().configureDataSources(dataSourcesBuildTimeConfig,
                dataSourcesJdbcBuildTimeConfig,
                dataSourcesRuntimeConfig, dataSourcesJdbcRuntimeConfig, disableSslSupport);
    }
}
