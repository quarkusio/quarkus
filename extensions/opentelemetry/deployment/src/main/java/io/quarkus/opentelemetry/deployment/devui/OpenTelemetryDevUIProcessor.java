package io.quarkus.opentelemetry.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.observability.ObservabilitySignalBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.UnlistedPageBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.TracesDevUiBuildTimeConfig;
import io.quarkus.opentelemetry.runtime.devui.DevUiTracesSpanProcessor;
import io.quarkus.opentelemetry.runtime.devui.DevUiTracesStoreProducer;
import io.quarkus.opentelemetry.runtime.devui.OpenTelemetryDevUIJsonRPCService;
// FIXME OTel must be enabled
public class OpenTelemetryDevUIProcessor {

    private static final String TRACES_TITLE = "Traces";
    // The tile shown in the core Observability section is prefixed with the backend so it
    // is clear which extension contributes it; the page itself stays titled "Traces".
    private static final String SIGNAL_TITLE = "OpenTelemetry Traces";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerDevUiBeans(TracesDevUiBuildTimeConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<JsonRPCProvidersBuildItem> jsonRpcProviders) {
        if (!config.enabled()) {
            return;
        }
        // These classes carry NO class-level scope annotation (so they are not
        // auto-discovered outside dev). Supply the scope here; @Singleton is fine.
        // The JSON-RPC service is NOT registered here — JsonRPCProvidersBuildItem
        // already registers it as a bean (as @ApplicationScoped); adding it here too
        // would clash with two different default scopes for the same class.
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(DevUiTracesStoreProducer.class,
                        DevUiTracesSpanProcessor.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
        jsonRpcProviders.produce(new JsonRPCProvidersBuildItem(OpenTelemetryDevUIJsonRPCService.class));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void tracesPage(TracesDevUiBuildTimeConfig config,
            BuildProducer<UnlistedPageBuildItem> unlistedPages,
            BuildProducer<ObservabilitySignalBuildItem> signals) {
        if (!config.enabled()) {
            return;
        }
        // The detail page is unlisted: reached only from the Observability section.
        UnlistedPageBuildItem page = new UnlistedPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:diagram-project")
                .title(TRACES_TITLE)
                .componentLink("qwc-otel-traces.js"));
        unlistedPages.produce(page);

        // Page id derived by Dev UI as "<namespace>/<title-dashed>" = "quarkus-opentelemetry/traces".
        signals.produce(new ObservabilitySignalBuildItem(
                "traces",
                SIGNAL_TITLE,
                "font-awesome-solid:diagram-project",
                "quarkus-opentelemetry/traces",
                "spanCount"));
    }
}
