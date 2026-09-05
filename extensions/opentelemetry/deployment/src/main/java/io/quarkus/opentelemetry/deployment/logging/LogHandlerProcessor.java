package io.quarkus.opentelemetry.deployment.logging;

import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.jandex.DotName;

import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.OpenTelemetrySdkBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogRecorder;
import io.quarkus.opentelemetry.runtime.logs.spi.LogsExporterCDIProvider;
import io.quarkus.runtime.RuntimeValue;

@BuildSteps(onlyIf = LogsEnabled.class)
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

    /**
     * The handler is created without waiting for the OpenTelemetry SDK (or the CDI container
     * it requires), so that runtime logging setup — which consumes all {@link LogHandlerBuildItem}s —
     * is not delayed until the SDK is ready. Delaying logging setup leaves every logger at the
     * initial all-enabling level while CDI startup and eagerly started services run, which
     * triggers debug/trace-only code paths, see
     * <a href="https://github.com/quarkusio/quarkus/issues/55889">GitHub issue #55889</a>.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(OpenTelemetryLogRecorder recorder,
            BuildProducer<LogHandlerBuildItem> logHandlerProducer,
            BuildProducer<OpenTelemetryLogHandlerBuildItem> otelLogHandlerProducer) {
        RuntimeValue<Optional<Handler>> handler = recorder.initializeHandler();
        logHandlerProducer.produce(new LogHandlerBuildItem(handler));
        otelLogHandlerProducer.produce(new OpenTelemetryLogHandlerBuildItem(handler));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(OpenTelemetrySdkBuildItem.class)
    void activate(OpenTelemetryLogRecorder recorder,
            OpenTelemetryLogHandlerBuildItem logHandler,
            BeanContainerBuildItem beanContainerBuildItem) {
        recorder.activateHandler(logHandler.getHandler(), beanContainerBuildItem.getValue());
    }
}
