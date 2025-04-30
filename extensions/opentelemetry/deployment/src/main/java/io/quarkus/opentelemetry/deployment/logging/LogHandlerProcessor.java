package io.quarkus.opentelemetry.deployment.logging;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.jandex.DotName;

import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogRecorder;
import io.quarkus.opentelemetry.runtime.logs.spi.LogsExporterCDIProvider;

@BuildSteps(onlyIf = LogHandlerProcessor.LogsEnabled.class)
class LogHandlerProcessor {

    private static final DotName LOG_RECORD_EXPORTER = DotName.createSimple(LogRecordExporter.class.getName());
    private static final DotName LOG_RECORD_PROCESSOR = DotName.createSimple(LogRecordProcessor.class.getName());

    @BuildStep
    void beanSupport(BuildProducer<UnremovableBeanBuildItem> unremovableProducer) {
        unremovableProducer.produce(UnremovableBeanBuildItem.beanTypes(LOG_RECORD_EXPORTER));
        unremovableProducer.produce(UnremovableBeanBuildItem.beanTypes(LOG_RECORD_PROCESSOR));
    }

    @BuildStep
    void nativeSupport(BuildProducer<ServiceProviderBuildItem> servicesProducer) {
        servicesProducer.produce(
                new ServiceProviderBuildItem(ConfigurableLogRecordExporterProvider.class.getName(),
                        LogsExporterCDIProvider.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(OpenTelemetrySdkBuildItem.class)
    LogHandlerBuildItem build(OpenTelemetryLogRecorder recorder,
            OTelRuntimeConfig config,
            BeanContainerBuildItem beanContainerBuildItem) {
        return new LogHandlerBuildItem(recorder.initializeHandler(beanContainerBuildItem.getValue(), config));
    }

    public static class LogsEnabled implements BooleanSupplier {
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.logs().enabled()
                    .map(new Function<Boolean, Boolean>() {
                        @Override
                        public Boolean apply(Boolean enabled) {
                            return otelBuildConfig.enabled() && enabled;
                        }
                    })
                    .orElseGet(() -> otelBuildConfig.enabled());
        }
    }
}
