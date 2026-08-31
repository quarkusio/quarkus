package io.quarkus.devui.deployment.observability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.observability.ObservabilitySignalBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Assembles the standalone "Observability" section in the Dev UI left menu from the
 * signals contributed by backend extensions (e.g. OpenTelemetry traces). The
 * section is a core-owned, cross-extension page rather than a per-extension card, so
 * it appears once regardless of which telemetry extensions are present.
 * <p>
 * The section landing page (qwc-observability-section.js) reads the
 * "observabilitySignals" build-time data and links to each signal's detail page.
 */
public class ObservabilitySectionProcessor {

    private static final String NAMESPACE = "devui-observability";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void observabilitySection(List<ObservabilitySignalBuildItem> signals,
            BuildProducer<InternalPageBuildItem> menuProducer) {
        if (signals.isEmpty()) {
            return;
        }

        List<Map<String, String>> signalData = new ArrayList<>();
        for (ObservabilitySignalBuildItem signal : signals) {
            signalData.add(Map.of(
                    "key", signal.getKey(),
                    "title", signal.getTitle(),
                    "icon", signal.getIcon(),
                    "pageId", signal.getPageId()));
        }

        InternalPageBuildItem page = new InternalPageBuildItem("Observability", 45);

        page.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .icon("font-awesome-solid:binoculars")
                .title("Observability")
                .componentLink("qwc-observability-section.js"));

        page.addBuildTimeData("observabilitySignals", signalData,
                "The telemetry signals (e.g. traces) contributed by observability extensions");

        menuProducer.produce(page);
    }
}
