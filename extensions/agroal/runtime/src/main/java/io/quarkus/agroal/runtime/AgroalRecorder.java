package io.quarkus.agroal.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.tm.XAResourceRecoveryRegistry;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
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

    @SuppressWarnings("deprecation")
    public Function<SyntheticCreationalContext<DataSources>, DataSources> dataSourcesSupplier(
            DataSourcesBuildTimeConfig buildTimeConfig, DataSourcesJdbcBuildTimeConfig jdbcBuildTimeConfig) {
        return new Function<>() {
            @Override
            public DataSources apply(SyntheticCreationalContext<DataSources> context) {
                var container = Arc.container();
                return new DataSources(
                        buildTimeConfig,
                        context.getInjectedReference(DataSourcesRuntimeConfig.class),
                        jdbcBuildTimeConfig,
                        context.getInjectedReference(DataSourcesJdbcRuntimeConfig.class),
                        container.instance(TransactionManagerConfiguration.class).get(),
                        container.instance(TransactionManager.class).get(),
                        container.instance(XAResourceRecoveryRegistry.class).get(),
                        container.instance(TransactionSynchronizationRegistry.class).get(),
                        context.getInjectedReference(AgroalDataSourceSupport.class),
                        container.select(AgroalPoolInterceptor.class, Any.Literal.INSTANCE),
                        container.select(AgroalOpenTelemetryWrapper.class));
            }
        };
    }

    public Supplier<AgroalOpenTelemetryWrapper> openTelemetryWrapperSupplier() {
        return new Supplier<>() {
            @Override
            public AgroalOpenTelemetryWrapper get() {
                AgroalOpenTelemetryWrapper wrapper = new AgroalOpenTelemetryWrapper();
                wrapper.openTelemetry = Arc.container().instance(
                        io.opentelemetry.api.OpenTelemetry.class).get();
                return wrapper;
            }
        };
    }

    public Function<SyntheticCreationalContext<AgroalDataSource>, AgroalDataSource> agroalDataSourceSupplier(
            String dataSourceName,
            Optional<RuntimeValue<Boolean>> otelEnabled,
            Map<String, String> jdbcProperties) {
        return new Function<>() {
            @SuppressWarnings("deprecation")
            @Override
            public AgroalDataSource apply(SyntheticCreationalContext<AgroalDataSource> context) {
                DataSources dataSources = context.getInjectedReference(DataSources.class);
                return dataSources.createDataSource(dataSourceName,
                        otelEnabled.isPresent() ? otelEnabled.get().getValue() : false, jdbcProperties);
            }
        };
    }
}
