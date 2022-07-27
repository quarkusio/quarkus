package io.quarkus.opentelemetry.exporter.jaeger.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterConfig;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterProvider;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerRecorder;

@BuildSteps(onlyIf = JaegerExporterProcessor.JaegerExporterEnabled.class)
public class JaegerExporterProcessor {

    static class JaegerExporterEnabled implements BooleanSupplier {
        JaegerExporterConfig.JaegerExporterBuildConfig jaegerExporterConfig;

        public boolean getAsBoolean() {
            return jaegerExporterConfig.enabled;
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
            JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig) {
        recorder.installBatchSpanProcessorForJaeger(runtimeConfig, launchModeBuildItem.getLaunchMode());
    }
}
