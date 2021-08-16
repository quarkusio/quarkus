package io.quarkus.agroal.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.agroal.api.AgroalDataSource;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesExcludedFromHealthChecks;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalRecorder {

    public Supplier<DataSourceSupport> dataSourceSupportSupplier(DataSourceSupport dataSourceSupport) {
        return new Supplier<DataSourceSupport>() {
            @Override
            public DataSourceSupport get() {
                return dataSourceSupport;
            }
        };
    }

    public Supplier<AgroalDataSource> agroalDataSourceSupplier(String dataSourceName,
            @SuppressWarnings("unused") DataSourcesRuntimeConfig dataSourcesRuntimeConfig) {
        final AgroalDataSource agroalDataSource = DataSources.fromName(dataSourceName);
        return new Supplier<AgroalDataSource>() {
            @Override
            public AgroalDataSource get() {
                return agroalDataSource;
            }
        };
    }

    public Supplier<DataSourcesExcludedFromHealthChecks> dataSourcesExcludedFromHealthChecks(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new Supplier<DataSourcesExcludedFromHealthChecks>() {
            @Override
            public DataSourcesExcludedFromHealthChecks get() {
                List<String> excludedNames = new ArrayList<>();
                if (dataSourcesBuildTimeConfig.defaultDataSource.healthExclude) {
                    excludedNames.add(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
                }
                for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : dataSourcesBuildTimeConfig.namedDataSources
                        .entrySet()) {
                    if (dataSource.getValue().healthExclude) {
                        excludedNames.add(dataSource.getKey());
                    }
                }
                return new DataSourcesExcludedFromHealthChecks(excludedNames);
            }
        };
    }
}
