package io.quarkus.devui.deployment.observability;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.runtime.observability.telemetry.TelemetryEventRouter;
import io.quarkus.devui.runtime.observability.telemetry.TelemetryEventsRecorder;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
import io.quarkus.devui.spi.observability.TracesBackendBuildItem;

/**
 * Receives the {@code io.quarkus.dev.telemetry.TelemetryEvent}s that metrics and tracing extensions fire, and hands
 * them to the stores behind the observability dashboard. The stores themselves are registered only when a backend
 * for their signal is present (see {@link MetricsDevUIProcessor} and {@link TracesDevUIProcessor}); an event for a
 * store that is not there is dropped. With no backend at all, nothing here is registered and events stay switched
 * off, so a sender does not build them for nobody.
 */
@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class TelemetryEventsProcessor {

    @BuildStep
    void registerRouter(List<MetricsBackendBuildItem> metricsBackends, List<TracesBackendBuildItem> tracesBackends,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (metricsBackends.isEmpty() && tracesBackends.isEmpty()) {
            return;
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(TelemetryEventRouter.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
    }

    /**
     * At static init, the earliest point: before any bean exists, so before anything can end a span or take a reading.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void enableTelemetryEvents(List<MetricsBackendBuildItem> metricsBackends,
            List<TracesBackendBuildItem> tracesBackends, TelemetryEventsRecorder recorder) {
        if (metricsBackends.isEmpty() && tracesBackends.isEmpty()) {
            return;
        }
        recorder.enable();
    }
}
