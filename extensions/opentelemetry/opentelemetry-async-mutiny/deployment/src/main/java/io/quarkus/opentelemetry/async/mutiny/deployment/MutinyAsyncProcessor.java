package io.quarkus.opentelemetry.async.mutiny.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.async.mutiny.runtime.MutinyAsyncConfig;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.MutinyAsyncRecorder;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.OpenTelemetryMultiInterceptor;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.OpenTelemetryUniInterceptor;

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
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerAsyncStrategy(final MutinyAsyncConfig.MutinyAsyncRuntimeConfig runtimeConfig,
            final MutinyAsyncRecorder recorder) {
        recorder.registerAsyncStrategy(runtimeConfig);
    }

    @BuildStep(onlyIf = MutinyAsyncEnabled.class)
    void registerUniInterceptor(
            final BuildProducer<NativeImageResourceBuildItem> resource,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.smallrye.mutiny.infrastructure.UniInterceptor"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, OpenTelemetryUniInterceptor.class));
    }

    @BuildStep(onlyIf = MutinyAsyncEnabled.class)
    void registerMultiInterceptor(
            final BuildProducer<NativeImageResourceBuildItem> resource,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.smallrye.mutiny.infrastructure.MultiInterceptor"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, OpenTelemetryMultiInterceptor.class));
    }
}
