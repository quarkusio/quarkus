package io.quarkus.opentelemetry.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.observability.TracesBackendBuildItem;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.config.build.TracesDevUiBuildTimeConfig;
import io.quarkus.opentelemetry.runtime.devui.DevUiTracesSpanProcessor;

/**
 * Sends the application's finished spans to the Dev UI traces view. OpenTelemetry only captures: the span processor
 * fires each span as a {@code span} telemetry event, and the core Dev UI owns the store, the JSON-RPC service and the
 * view that show it.
 */
@BuildSteps(onlyIf = { OpenTelemetryEnabled.class, IsLocalDevelopment.class })
public class OpenTelemetryDevUIProcessor {

    // The tile shown in the core Observability section names the backend, so it is clear which extension
    // contributes the spans.
    private static final String SIGNAL_TITLE = "OpenTelemetry Traces";

    @BuildStep
    void registerSpanCapture(TracesDevUiBuildTimeConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<TracesBackendBuildItem> backends) {
        if (!config.enabled()) {
            return;
        }
        // The processor carries NO class-level scope annotation (so it is not auto-discovered outside dev).
        // Supply the scope here; @Singleton is fine.
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(DevUiTracesSpanProcessor.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
        backends.produce(new TracesBackendBuildItem("otel", SIGNAL_TITLE));
    }
}
