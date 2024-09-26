package io.quarkus.agroal.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalRecorder {

    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;

    @Inject
    public AgroalRecorder(RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Supplier<AgroalDataSourceSupport> dataSourceSupportSupplier(AgroalDataSourceSupport agroalDataSourceSupport) {
        return new Supplier<AgroalDataSourceSupport>() {
            @Override
            public AgroalDataSourceSupport get() {
                return agroalDataSourceSupport;
            }
        };
    }

    public Supplier<ActiveResult> agroalDataSourceCheckActiveSupplier(String dataSourceName) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                if (!runtimeConfig.getValue().dataSources().get(dataSourceName).active()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonDeactivated(dataSourceName));
                }

                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<AgroalDataSource>, AgroalDataSource> agroalDataSourceSupplier(
            String dataSourceName,
            @SuppressWarnings("unused") DataSourcesRuntimeConfig dataSourcesRuntimeConfig) {
        return new Function<>() {
            @SuppressWarnings("deprecation")
            @Override
            public AgroalDataSource apply(SyntheticCreationalContext<AgroalDataSource> context) {
                DataSources dataSources = context.getInjectedReference(DataSources.class);
                return dataSources.createDataSource(dataSourceName);
            }
        };
    }
}
