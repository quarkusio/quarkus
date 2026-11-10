package io.quarkus.reactive.datasource.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

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
                return ReactivePoolUtil.checkActive(dataSourceName, runtimeConfig.getValue(),
                        reactiveRuntimeConfig.getValue());
            }
        };
    }
}
