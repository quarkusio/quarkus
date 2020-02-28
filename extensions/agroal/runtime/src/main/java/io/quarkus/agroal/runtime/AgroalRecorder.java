package io.quarkus.agroal.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourcesRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalRecorder {

    public void configureDatasources(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourcesJdbcRuntimeConfig legacyDataSourcesJdbcRuntimeConfig,
            boolean disableSslSupport) {
        Arc.container().instance(AbstractDataSourceProducer.class).get().configureDataSources(dataSourcesBuildTimeConfig,
                dataSourcesJdbcBuildTimeConfig, dataSourcesRuntimeConfig, dataSourcesJdbcRuntimeConfig,
                legacyDataSourcesJdbcBuildTimeConfig, legacyDataSourcesRuntimeConfig, legacyDataSourcesJdbcRuntimeConfig,
                disableSslSupport);
    }
}
