package io.quarkus.agroal.runtime;

import java.util.function.Supplier;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
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
        final AgroalDataSource agroalDataSource = Arc.container().instance(DataSources.class).get()
                .createDataSource(dataSourceName);
        return new Supplier<AgroalDataSource>() {
            @Override
            public AgroalDataSource get() {
                return agroalDataSource;
            }
        };
    }

}
