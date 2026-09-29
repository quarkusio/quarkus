package io.quarkus.devui.deployment.observability;

import java.util.List;
import java.util.TreeMap;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.observability.traces.TracesDevUIJsonRPCService;
import io.quarkus.devui.runtime.observability.traces.TracesStoreProducer;
import io.quarkus.devui.spi.observability.ObservabilitySignalBuildItem;
import io.quarkus.devui.spi.observability.TracesBackendBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;

/**
 * The traces view of the observability dashboard. It belongs to Dev UI rather than to a tracer: a tracer only fires
 * its finished spans as events and produces a {@link TracesBackendBuildItem}, and this registers what receives and
 * shows them. With no tracer, none of it is registered.
 */
@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class TracesDevUIProcessor {

    static final String NAMESPACE = "devui-observability-traces";
    private static final String TITLE = "Traces";
    private static final String ICON = "font-awesome-solid:diagram-project";

    @BuildStep
    void registerTracesStore(List<TracesBackendBuildItem> backends,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<JsonRPCProvidersBuildItem> jsonRpcProviders) {
        if (backends.isEmpty()) {
            return;
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(TracesStoreProducer.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
        jsonRpcProviders.produce(new JsonRPCProvidersBuildItem(NAMESPACE, TracesDevUIJsonRPCService.class));
    }

    @BuildStep
    void tracesView(List<TracesBackendBuildItem> backends,
            BuildProducer<InternalPageBuildItem> pages,
            BuildProducer<ObservabilitySignalBuildItem> signals) {
        if (backends.isEmpty()) {
            return;
        }
        // The full page, which the dashboard's traces card opens and embeds. Unlisted: it is reached from the
        // Observability dashboard, not from the menu.
        PageBuilder<?> page = Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .icon(ICON)
                .title(TITLE)
                .componentLink("qwc-observability-traces.js");
        InternalPageBuildItem tracesPage = new InternalPageBuildItem(TITLE, 0);
        tracesPage.addUnlistedPage(page);
        pages.produce(tracesPage);

        signals.produce(new ObservabilitySignalBuildItem("traces", signalTitle(backends), ICON,
                tracesPage.getUnlistedPages().get(0).getId(), "spanCount"));
    }

    /**
     * The title of the traces card, which names the tracer that sends the spans. With several - unusual, but each is
     * free to produce the build item - the build order must not decide which one the card is named after, so a
     * single title covers them all.
     */
    static String signalTitle(List<TracesBackendBuildItem> backends) {
        // By source, so that the order the build items were produced in does not matter.
        TreeMap<String, String> titles = new TreeMap<>();
        for (TracesBackendBuildItem backend : backends) {
            titles.putIfAbsent(backend.getSource(), backend.getTitle());
        }
        if (titles.size() == 1) {
            return titles.firstEntry().getValue();
        }
        return TITLE + " (" + String.join(", ", titles.keySet()) + ")";
    }
}
