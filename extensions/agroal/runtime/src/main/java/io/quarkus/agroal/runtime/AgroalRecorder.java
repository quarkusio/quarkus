package io.quarkus.agroal.runtime;

import java.util.Optional;
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
    private final RuntimeValue<DataSourcesJdbcRuntimeConfig> jdbcRuntimeConfig;

    @Inject
    public AgroalRecorder(RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig,
            RuntimeValue<DataSourcesJdbcRuntimeConfig> jdbcRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.jdbcRuntimeConfig = jdbcRuntimeConfig;
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
                Optional<Boolean> active = runtimeConfig.getValue().dataSources().get(dataSourceName).active();
                if (active.isPresent() && !active.get()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonDeactivated(dataSourceName));
                }
                if (jdbcRuntimeConfig.getValue().dataSources().get(dataSourceName).jdbc().url().isEmpty()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonUrlMissing(dataSourceName,
                            "jdbc.url"));
                }
                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<AgroalDataSource>, AgroalDataSource> agroalDataSourceSupplier(
            String dataSourceName,
            Optional<RuntimeValue<Boolean>> otelEnabled) {
        return new Function<>() {
            @SuppressWarnings("deprecation")
            @Override
            public AgroalDataSource apply(SyntheticCreationalContext<AgroalDataSource> context) {
                DataSources dataSources = context.getInjectedReference(DataSources.class);
                return dataSources.createDataSource(dataSourceName,
                        otelEnabled.isPresent() ? otelEnabled.get().getValue() : false);
            }
        };
    }
}
