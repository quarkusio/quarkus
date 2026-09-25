package io.quarkus.devui.deployment.observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
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
            List<MetricsBackendBuildItem> metricsBackends,
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
        // Null when the metrics of this application do not reach Prometheus under a naming known here, in
        // which case the dashboard offers no Grafana export rather than an export with wrong queries.
        page.addBuildTimeData("prometheusNaming", prometheusNaming(metricsBackends),
                "How this application's metrics are named once exported to Prometheus, "
                        + "used to export the dashboard for Grafana");

        menuProducer.produce(page);
    }

    /**
     * The export naming shared by the metrics backends. With more than one backend answering differently
     * there is no single set of names to query, so no export is offered.
     */
    private static String prometheusNaming(List<MetricsBackendBuildItem> backends) {
        String naming = null;
        for (MetricsBackendBuildItem backend : backends) {
            if (backend.getPrometheusNaming() == null) {
                return null;
            }
            if (naming != null && !naming.equals(backend.getPrometheusNaming())) {
                return null;
            }
            naming = backend.getPrometheusNaming();
        }
        return naming;
    }
}
