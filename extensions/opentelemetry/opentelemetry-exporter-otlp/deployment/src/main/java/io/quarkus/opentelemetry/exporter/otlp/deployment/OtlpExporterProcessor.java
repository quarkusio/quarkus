package io.quarkus.opentelemetry.exporter.otlp.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterBuildConfig;
import io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterProvider;
import io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpRecorder;
import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.OtelRuntimeConfig;

@BuildSteps(onlyIf = OtlpExporterProcessor.OtlpExporterEnabled.class)
public class OtlpExporterProcessor {

    static class OtlpExporterEnabled implements BooleanSupplier {
        OtlpExporterBuildConfig exporBuildConfig;
        OtelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled() &&
                    otelBuildConfig.traces().enabled().orElse(Boolean.TRUE) &&
                    //                    otelBuildConfig.traces().exporter().contains("otlp") &&
                    exporBuildConfig.enabled().orElse(Boolean.TRUE);
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
            OtelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        recorder.installBatchSpanProcessorForOtlp(otelRuntimeConfig,
                exporterRuntimeConfig,
                launchModeBuildItem.getLaunchMode());
    }
}
