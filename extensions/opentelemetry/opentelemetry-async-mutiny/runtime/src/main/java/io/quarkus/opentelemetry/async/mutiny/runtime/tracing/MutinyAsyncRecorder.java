package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategies;
import io.quarkus.opentelemetry.async.mutiny.runtime.MutinyAsyncConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MutinyAsyncRecorder {

    public void registerAsyncStrategy(final MutinyAsyncConfig.MutinyAsyncRuntimeConfig runtimeConfig) {
        final MutinyAsyncOperationEndStrategy mutinyAsyncOperationEndStrategy = new MutinyAsyncOperationEndStrategy(
                runtimeConfig);
        AsyncOperationEndStrategies.instance().registerStrategy(mutinyAsyncOperationEndStrategy);
    }
}
