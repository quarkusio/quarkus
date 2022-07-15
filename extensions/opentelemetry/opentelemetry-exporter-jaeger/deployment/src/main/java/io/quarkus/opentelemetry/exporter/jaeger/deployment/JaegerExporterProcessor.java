package io.quarkus.opentelemetry.exporter.jaeger.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterBuildConfig;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterProvider;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterRuntimeConfig;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerRecorder;
import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.OtelRuntimeConfig;

@BuildSteps(onlyIf = JaegerExporterProcessor.JaegerExporterEnabled.class)
public class JaegerExporterProcessor {

    static class JaegerExporterEnabled implements BooleanSupplier {
        JaegerExporterBuildConfig jaegerExporterConfig;
        OtelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled() &&
                    otelBuildConfig.traces().enabled().orElse(Boolean.TRUE) &&
                    //                        otelBuildConfig.traces().exporter().contains("jaeger") &&
                    jaegerExporterConfig.enabled().orElse(Boolean.TRUE);
        }

    }

    @BuildStep
    AdditionalBeanBuildItem createBatchSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(JaegerExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void installBatchSpanProcessorForJaeger(JaegerRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            OtelRuntimeConfig otelRuntimeConfig,
            JaegerExporterRuntimeConfig runtimeConfig) {
        recorder.installBatchSpanProcessorForJaeger(otelRuntimeConfig, runtimeConfig, launchModeBuildItem.getLaunchMode());
    }
}
