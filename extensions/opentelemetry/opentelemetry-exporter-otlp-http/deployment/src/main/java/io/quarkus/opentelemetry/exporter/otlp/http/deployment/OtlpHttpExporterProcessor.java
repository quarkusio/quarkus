package io.quarkus.opentelemetry.exporter.otlp.http.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.exporter.otlp.http.runtime.OtlpHttpExporterConfig;
import io.quarkus.opentelemetry.exporter.otlp.http.runtime.OtlpHttpExporterProvider;
import io.quarkus.opentelemetry.exporter.otlp.http.runtime.OtlpHttpRecorder;

public class OtlpHttpExporterProcessor {

    static class OtlpHttpExporterEnabled implements BooleanSupplier {
        OtlpHttpExporterConfig.OtlpHttpExporterBuildConfig otlpHttpExporterConfig;

        public boolean getAsBoolean() {
            return otlpHttpExporterConfig.enabled;
        }
    }

    @BuildStep(onlyIf = OtlpHttpExporterEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY_OTLP_HTTP_EXPORTER);
    }

    @BuildStep(onlyIf = OtlpHttpExporterEnabled.class)
    AdditionalBeanBuildItem createBatchSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(OtlpHttpExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep(onlyIf = OtlpHttpExporterEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void installBatchSpanProcessorForOtlpHttp(OtlpHttpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            OtlpHttpExporterConfig.OtlpHttpExporterRuntimeConfig runtimeConfig) {
        recorder.installBatchSpanProcessorForOtlpHttp(runtimeConfig, launchModeBuildItem.getLaunchMode());
    }
}
