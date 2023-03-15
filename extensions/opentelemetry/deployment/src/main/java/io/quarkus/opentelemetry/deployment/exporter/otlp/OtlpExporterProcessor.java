package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;
import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.OTLP_VALUE;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OtelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.exporter.OtlpExporterBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OtelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpExporterProvider;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpRecorder;

@BuildSteps(onlyIf = OtlpExporterProcessor.OtlpExporterEnabled.class)
public class OtlpExporterProcessor {

    static class OtlpExporterEnabled implements BooleanSupplier {
        OtlpExporterBuildConfig exporBuildConfig;
        OtelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled &&
                    otelBuildConfig.traces.enabled.orElse(Boolean.TRUE) &&
                    (otelBuildConfig.traces.exporter.contains(OTLP_VALUE) ||
                            otelBuildConfig.traces.exporter.contains(CDI_VALUE))
                    &&
                    exporBuildConfig.enabled;
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
    void installBatchSpanProcessorForOtlp(
            OtlpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            OtelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            BeanContainerBuildItem beanContainerBuildItem) {
        recorder.installBatchSpanProcessorForOtlp(otelRuntimeConfig,
                exporterRuntimeConfig,
                launchModeBuildItem.getLaunchMode());
    }
}
