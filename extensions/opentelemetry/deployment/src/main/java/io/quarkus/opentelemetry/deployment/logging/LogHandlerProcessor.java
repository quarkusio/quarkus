package io.quarkus.opentelemetry.deployment.logging;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;

import org.jboss.jandex.DotName;

import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogHandler;
import io.quarkus.opentelemetry.runtime.logs.spi.LogsExporterCDIProvider;
import io.quarkus.runtime.logging.LogCleanupFilter;
import io.quarkus.vertx.VertxSupplier;

@BuildSteps(onlyIf = LogsEnabled.class)
class LogHandlerProcessor {

    private static final String HANDLER_NAME = "opentelemetry";
    private static final String STARTUP_EVENT_SERVICE_NAME = "io.quarkus.arc.startup-event";

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
     * it requires), so that runtime logging setup — which consumes all {@link Handler} services —
     * is not delayed until the SDK is ready. Delaying logging setup leaves every logger at the
     * initial all-enabling level while CDI startup and eagerly started services run, which
     * triggers debug/trace-only code paths, see
     * <a href="https://github.com/quarkusio/quarkus/issues/55889">GitHub issue #55889</a>.
     */
    @BuildStep
    void build(ServiceRegistrar serviceRegistrar, LaunchModeBuildItem launchMode) {
        // Logging is not set up in an auxiliary application, such as the one running continuous tests,
        // unless it runs the tests only: the error manager and the cleanup filter do not exist there.
        // The same condition as in LoggingResourceProcessor#setupLoggingRuntimeInit
        if (launchMode.isAuxiliaryApplication()
                && launchMode.getAuxiliaryDevModeType().orElse(null) != DevModeType.TEST_ONLY) {
            return;
        }
        serviceRegistrar.forService(Handler.class, HANDLER_NAME)
                .atPhase(Phase.LOGGING)
                .require(OTelRuntimeConfig.class)
                .require(ErrorManager.class)
                .require(LogCleanupFilter.class)
                .optional()
                .onStart((ctx, runtimeConfig, errorManager, cleanupFilter) -> OpenTelemetryLogHandler
                        .createHandler(runtimeConfig, errorManager, cleanupFilter));
    }

    /**
     * Activates the handler once the CDI beans are ready, and before the HTTP socket opens and the startup event is
     * fired: the startup event by a service edge, the socket opening by the declared {@link ServiceStartBuildItem}.
     * Getting the {@code OpenTelemetry} bean creates the SDK and the exporters, which need Vert.x.
     */
    @SuppressWarnings("deprecation")
    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void activate(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService("io.quarkus.opentelemetry.activate-log-handler")
                .request(Handler.class, HANDLER_NAME)
                .require(BeanContainer.class)
                .require(VertxSupplier.class)
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                // before the startup event, declared as a service edge; ServiceStartBuildItem is declared only to come
                // before the socket opening, which is still a recorder step
                .before(STARTUP_EVENT_SERVICE_NAME)
                .onStart((ctx, handler, beanContainer, vertx) -> OpenTelemetryLogHandler.activateHandler(handler,
                        beanContainer));
    }
}
