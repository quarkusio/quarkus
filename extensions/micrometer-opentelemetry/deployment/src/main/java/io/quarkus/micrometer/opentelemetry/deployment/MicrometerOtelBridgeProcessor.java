package io.quarkus.micrometer.opentelemetry.deployment;

import java.util.Locale;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logmanager.Level;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.opentelemetry.runtime.MicrometerOtelBridgeRecorder;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;

@BuildSteps(onlyIf = {
        MicrometerProcessor.MicrometerEnabled.class,
        OpenTelemetryEnabled.class,
        MicrometerOtelBridgeProcessor.OtlpMetricsExporterEnabled.class })
public class MicrometerOtelBridgeProcessor {

    @BuildStep
    public void disableOTelAutoInstrumentedMetrics(BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigProducer) {
        runtimeConfigProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.otel.instrument.http-server-metrics", "false"));
        runtimeConfigProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.otel.instrument.jvm-metrics", "false"));
    }

    @BuildStep
    public void tuneDefaultConfigs(BuildProducer<LogCategoryBuildItem> logCategoryProducer) {
        // Suppress noisy logs from Micrometer:
        // ...A MeterFilter is being configured after a Meter has been registered to this registry...
        logCategoryProducer.produce(new LogCategoryBuildItem(
                "io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry",
                Level.ERROR));
        logCategoryProducer.produce(new LogCategoryBuildItem(
                "io.micrometer.core.instrument.composite.CompositeMeterRegistry",
                Level.ERROR));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBridgeBean(
            OTelBuildConfig oTelBuildConfig,
            MicrometerOtelBridgeRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer) {

        if (!oTelBuildConfig.enabled()) {
            return; // No point in creating the bridge if the SDK is disabled
        }

        syntheticBeanProducer.produce(SyntheticBeanBuildItem.configure(MeterRegistry.class)
                .defaultBean()
                .setRuntimeInit()
                .unremovable()
                .scope(Singleton.class)
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(OpenTelemetry.class.getName())) }, null))
                .createWith(recorder.createBridge())
                .done());
    }

    /**
     * No point in activating the bridge if the OTel metrics if off or the exporter is none.
     */
    static class OtlpMetricsExporterEnabled implements BooleanSupplier {
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.metrics().enabled().orElse(Boolean.TRUE) &&
                    !otelBuildConfig.metrics().exporter().stream()
                            .map(exporter -> exporter.toLowerCase(Locale.ROOT))
                            .anyMatch(exporter -> exporter.contains("none"));
        }
    }
}
