package io.quarkus.reactive.datasource.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.sqlclient.Pool;

/**
 * Recorder for generic reactive pool operations shared across all SQL client extensions.
 */
@Recorder
public class ReactivePoolRecorder {

    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;
    private final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig;

    public ReactivePoolRecorder(
            RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig,
            RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.reactiveRuntimeConfig = reactiveRuntimeConfig;
    }

    public Supplier<ActiveResult> poolCheckActiveSupplier(String dataSourceName) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                Optional<Boolean> active = runtimeConfig.getValue().dataSources().get(dataSourceName).active();
                if (active.isPresent() && !active.get()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonDeactivated(dataSourceName));
                }
                if (reactiveRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().url().isEmpty()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonUrlMissing(dataSourceName,
                            "reactive.url"));
                }
                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<io.vertx.mutiny.sqlclient.Pool>, io.vertx.mutiny.sqlclient.Pool> mutinyPool(
            String dataSourceName) {
        return new Function<>() {
            @SuppressWarnings("unchecked")
            @Override
            public io.vertx.mutiny.sqlclient.Pool apply(SyntheticCreationalContext context) {
                return io.vertx.mutiny.sqlclient.Pool.newInstance(
                        (Pool) context.getInjectedReference(Pool.class, qualifier(dataSourceName)));
            }
        };
    }

    public RuntimeValue<ReactivePoolsHealthConfig> createHealthConfig(Map<String, String> healthCheckSqlByDatasource) {
        return new RuntimeValue<>(new ReactivePoolsHealthConfig(healthCheckSqlByDatasource));
    }
}
