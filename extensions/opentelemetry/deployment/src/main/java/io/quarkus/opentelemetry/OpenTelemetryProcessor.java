package io.quarkus.opentelemetry;

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
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.tracing.TracerProviderBuildItem;
import io.quarkus.runtime.RuntimeValue;

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
    void createOpenTelemetry(OpenTelemetryRecorder recorder, Optional<TracerProviderBuildItem> tracerProviderBuildItem) {
        RuntimeValue<SdkTracerProvider> tracerProvider = tracerProviderBuildItem.map(TracerProviderBuildItem::getTracerProvider)
                .orElse(null);
        recorder.createOpenTelemetry(tracerProvider);
    }
}
