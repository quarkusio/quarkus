package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.exporter.OtlpExporterBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfigBuilder;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterRecorder;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

@BuildSteps
public class OtlpExporterProcessor {

    private static final DotName METRIC_EXPORTER = DotName.createSimple(MetricExporter.class.getName());
    private static final DotName LOG_RECORD_EXPORTER = DotName.createSimple(LogRecordExporter.class.getName());

    static class OtlpTracingExporterEnabled implements BooleanSupplier {
        OtlpExporterBuildConfig exportBuildConfig;
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled() &&
                    otelBuildConfig.traces().enabled().orElse(Boolean.TRUE) &&
                    otelBuildConfig.traces().exporter().contains(CDI_VALUE) &&
                    exportBuildConfig.enabled();
        }
    }

    static class OtlpMetricsExporterEnabled implements BooleanSupplier {
        OtlpExporterBuildConfig exportBuildConfig;
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled() &&
                    otelBuildConfig.metrics().enabled().orElse(Boolean.TRUE) &&
                    otelBuildConfig.metrics().exporter().contains(CDI_VALUE) &&
                    exportBuildConfig.enabled();
        }
    }

    static class OtlpLogRecordExporterEnabled implements BooleanSupplier {
        OtlpExporterBuildConfig exportBuildConfig;
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.enabled() &&
                    otelBuildConfig.logs().enabled().orElse(Boolean.TRUE) &&
                    otelBuildConfig.logs().exporter().contains(CDI_VALUE) &&
                    exportBuildConfig.enabled();
        }
    }

    @BuildStep
    void logging(BuildProducer<LogCategoryBuildItem> log) {
        // Reduce the log level of the exporters because it's too much, and we do log important things ourselves.
        log.produce(new LogCategoryBuildItem("io.opentelemetry.exporter.internal.grpc.GrpcExporter", Level.OFF));
        log.produce(new LogCategoryBuildItem("io.opentelemetry.exporter.internal.http.HttpExporter", Level.OFF));
    }

    @BuildStep
    void config(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilderProducer) {
        runTimeConfigBuilderProducer.produce(new RunTimeConfigBuilderBuildItem(OtlpExporterConfigBuilder.class));
    }

    @SuppressWarnings("deprecation")
    @BuildStep(onlyIf = OtlpExporterProcessor.OtlpTracingExporterEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(TlsRegistryBuildItem.class)
    void createSpanProcessor(OTelExporterRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            List<ExternalOtelExporterBuildItem> externalOtelExporterBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (!externalOtelExporterBuildItem.isEmpty()) {
            // if there is an external exporter, we don't want to create the default one
            return;
        }
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(LateBoundSpanProcessor.class)
                .types(SpanProcessor.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(SpanExporter.class.getName())) }, null))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TlsConfigurationRegistry.class)))
                .createWith(recorder.spanProcessorForOtlp(vertxBuildItem.getVertx()))
                .done());
    }

    @BuildStep(onlyIf = OtlpMetricsExporterEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(TlsRegistryBuildItem.class)
    void createMetricsExporterProcessor(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            OTelExporterRecorder recorder,
            List<ExternalOtelExporterBuildItem> externalOtelExporterBuildItem,
            CoreVertxBuildItem vertxBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        if (!externalOtelExporterBuildItem.isEmpty()) {
            // if there is an external exporter, we don't want to create the default one.
            // External exporter also use synthetic beans. However, synthetic beans don't show in the BeanDiscoveryFinishedBuildItem
            return;
        }

        if (!beanDiscovery.beanStream().withBeanType(METRIC_EXPORTER).isEmpty()) {
            // if there is a MetricExporter bean impl around, we don't want to create the default one
            return;
        }

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(MetricExporter.class)
                .types(MetricExporter.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(MetricExporter.class.getName())) }, null))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TlsConfigurationRegistry.class)))
                .createWith(recorder.createMetricExporter(vertxBuildItem.getVertx()))
                .done());
    }

    @BuildStep(onlyIf = OtlpLogRecordExporterEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(TlsRegistryBuildItem.class)
    void createLogRecordExporterProcessor(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            OTelExporterRecorder recorder,
            List<ExternalOtelExporterBuildItem> externalOtelExporterBuildItem,
            CoreVertxBuildItem vertxBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        if (!externalOtelExporterBuildItem.isEmpty()) {
            // if there is an external exporter, we don't want to create the default one.
            // External exporter also use synthetic beans. However, synthetic beans don't show in the BeanDiscoveryFinishedBuildItem
            return;
        }

        if (!beanDiscovery.beanStream().withBeanType(LOG_RECORD_EXPORTER).isEmpty()) {
            // if there is a MetricExporter bean impl around, we don't want to create the default one
            return;
        }

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(LogRecordExporter.class)
                .types(LogRecordExporter.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(LogRecordExporter.class.getName())) }, null))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TlsConfigurationRegistry.class)))
                .createWith(recorder.createLogRecordExporter(vertxBuildItem.getVertx()))
                .done());
    }
}
