package io.quarkus.opentelemetry.deployment;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerProviderBuildItem;
import io.quarkus.opentelemetry.runtime.OpenTelemetryConfig;
import io.quarkus.opentelemetry.runtime.OpenTelemetryProducer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

public class OpenTelemetryProcessor {

    static class OpenTelemetryEnabled implements BooleanSupplier {
        OpenTelemetryConfig otelConfig;

        public boolean getAsBoolean() {
            return otelConfig.enabled;
        }
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY);
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(OpenTelemetryProducer.class)
                .build();
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void registerOpenTelemetryContextStorage(
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.opentelemetry.context.ContextStorageProvider"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, QuarkusContextStorage.class));
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void createOpenTelemetry(OpenTelemetryConfig openTelemetryConfig,
            OpenTelemetryRecorder recorder,
            Optional<TracerProviderBuildItem> tracerProviderBuildItem,
            LaunchModeBuildItem launchMode) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        RuntimeValue<SdkTracerProvider> tracerProvider = tracerProviderBuildItem.map(TracerProviderBuildItem::getTracerProvider)
                .orElse(null);
        recorder.createOpenTelemetry(tracerProvider, openTelemetryConfig);
        recorder.eagerlyCreateContextStorage();
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void storeVertxOnContextStorage(OpenTelemetryRecorder recorder, CoreVertxBuildItem vertx) {
        recorder.storeVertxOnContextStorage(vertx.getVertx());
    }
}
