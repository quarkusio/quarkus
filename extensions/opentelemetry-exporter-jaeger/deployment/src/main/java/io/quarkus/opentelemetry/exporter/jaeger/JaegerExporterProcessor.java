package io.quarkus.opentelemetry.exporter.jaeger;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class JaegerExporterProcessor {

    static class JaegerExporterEnabled implements BooleanSupplier {
        JaegerExporterConfig.JaegerExporterBuildConfig jaegerExporterConfig;

        public boolean getAsBoolean() {
            return jaegerExporterConfig.enabled;
        }
    }

    @BuildStep(onlyIf = JaegerExporterEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY_JAEGER_EXPORTER);
    }

    @BuildStep(onlyIf = JaegerExporterEnabled.class)
    AdditionalBeanBuildItem createBatchSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(JaegerExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep(onlyIf = JaegerExporterEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void installBatchSpanProcessorForJaeger(JaegerRecorder recorder,
            JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig) {
        recorder.installBatchSpanProcessorForJaeger(runtimeConfig);
    }
}
