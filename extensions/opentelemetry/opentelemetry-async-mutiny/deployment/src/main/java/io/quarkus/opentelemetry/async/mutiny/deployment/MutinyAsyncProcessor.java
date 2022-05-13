package io.quarkus.opentelemetry.async.mutiny.deployment;

import java.util.function.BooleanSupplier;

import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategies;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.opentelemetry.async.mutiny.runtime.MutinyAsyncConfig;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.MutinyAsyncOperationEndStrategy;

public class MutinyAsyncProcessor {

    static class MutinyAsyncEnabled implements BooleanSupplier {
        MutinyAsyncConfig.MutinyAsyncBuildConfig mutinyAsyncConfig;

        public boolean getAsBoolean() {
            return mutinyAsyncConfig.enabled;
        }
    }

    @BuildStep(onlyIf = MutinyAsyncEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY_MUTINY_ASYNC);
    }

    @BuildStep(onlyIf = MutinyAsyncEnabled.class)
    void registerAsyncStrategy(final MutinyAsyncConfig.MutinyAsyncRuntimeConfig runtimeConfig) {

        final MutinyAsyncOperationEndStrategy mutinyAsyncOperationEndStrategy = new MutinyAsyncOperationEndStrategy(
                runtimeConfig);
        AsyncOperationEndStrategies.instance().registerStrategy(mutinyAsyncOperationEndStrategy);
    }
}
