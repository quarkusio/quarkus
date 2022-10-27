package io.quarkus.opentelemetry.deployment.exporter.otlp;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpExporterProvider;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpRecorder;

@BuildSteps(onlyIf = OtlpExporterProcessor.OtlpExporterEnabled.class)
public class OtlpExporterProcessor {

    static class OtlpExporterEnabled implements BooleanSupplier {
        OtlpExporterConfig.OtlpExporterBuildConfig otlpExporterConfig;

        public boolean getAsBoolean() {
            return otlpExporterConfig.enabled;
        }
    }

    @BuildStep
    AdditionalBeanBuildItem createBatchSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(OtlpExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void installBatchSpanProcessorForOtlp(OtlpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            OtlpExporterConfig.OtlpExporterRuntimeConfig runtimeConfig) {
        recorder.installBatchSpanProcessorForOtlp(runtimeConfig, launchModeBuildItem.getLaunchMode());
    }
}
