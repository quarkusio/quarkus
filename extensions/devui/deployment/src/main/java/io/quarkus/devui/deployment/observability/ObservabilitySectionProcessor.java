package io.quarkus.devui.deployment.observability;

import java.util.ArrayList;
import java.util.HashMap;
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
 * The section is a single page (qwc-observability-dashboard.js): a dashboard the user
 * composes from the available cards. It reads the "observabilitySignals" build-time data to
 * discover what is on offer and stores the chosen cards in browser LocalStorage.
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
            // Not Map.of: pageId is optional and Map.of rejects null values.
            Map<String, String> data = new HashMap<>();
            data.put("key", signal.getKey());
            data.put("title", signal.getTitle());
            data.put("icon", signal.getIcon());
            if (signal.getPageId() != null) {
                data.put("pageId", signal.getPageId());
            }
            signalData.add(data);
        }

        InternalPageBuildItem page = new InternalPageBuildItem("Observability", 45);

        page.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .icon("font-awesome-solid:binoculars")
                .title("Observability")
                .componentLink("qwc-observability-dashboard.js"));

        page.addBuildTimeData("observabilitySignals", signalData,
                "The telemetry signals (e.g. traces) contributed by observability extensions");

        menuProducer.produce(page);
    }
}
