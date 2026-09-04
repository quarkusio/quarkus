package io.quarkus.devui.deployment.observability;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.runtime.observability.metrics.MetricsDevUIJsonRPCService;
import io.quarkus.devui.runtime.observability.metrics.MetricsStoreProducer;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
import io.quarkus.devui.spi.observability.ObservabilitySignalBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.UnlistedPageBuildItem;

/**
 * Registers the unified Dev UI metrics store producer + JSON-RPC service + page + Observability
 * signal when at least one backend adapter contributed a {@link MetricsBackendBuildItem}. One
 * page / one signal regardless of how many backends are active (natural dedup). Backends produce
 * the build item only when the metrics feature is enabled (see Tasks C2/D2), so no config read
 * is needed here.
 * <p>
 * {@code MetricsStoreProducer} and {@code MetricsDevUIJsonRPCService} live in the
 * {@code quarkus-devui} runtime (already a dependency of this module) and are on the application
 * runtime classpath in dev; both carry no class-level scope, so this build step is what turns
 * them into beans (dev-only). Mirrors {@code OpenTelemetryDevUIProcessor}.
 */
public class MetricsDevUIProcessor {

    private static final String NAMESPACE = "devui-observability";
    private static final String TITLE = "Metrics";
    private static final String ICON = "font-awesome-solid:chart-line";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerMetricsDevUI(List<MetricsBackendBuildItem> backends,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<JsonRPCProvidersBuildItem> jsonRpcProviders,
            BuildProducer<UnlistedPageBuildItem> unlistedPages,
            BuildProducer<ObservabilitySignalBuildItem> signals) {
        if (backends.isEmpty()) {
            return;
        }

        // Turn the (scope-less) store producer into a bean; @Produces method supplies @Singleton.
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(MetricsStoreProducer.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());

        // Register the service as a bean (@ApplicationScoped) and expose its public methods as
        // JSON-RPC UNDER THE EXPLICIT "devui-observability" NAMESPACE — must match the page's
        // namespace (so the page's JsonRpc client and the tests' super("devui-observability")
        // resolve). The service itself carries no scope annotation.
        jsonRpcProviders.produce(new JsonRPCProvidersBuildItem(NAMESPACE, MetricsDevUIJsonRPCService.class));

        // Unlisted: reached from the Observability section only. The web component (qwc-metrics.js)
        // is a CORE devui resource served from the internal "qwc/" bucket, so the page MUST be
        // marked internal() — otherwise Dev UI resolves the component under the namespace path
        // (/q/dev-ui/devui-observability/qwc-metrics.js), which nothing serves, and the page stays
        // blank. The explicit namespace is still required so the page's JsonRpc client targets the
        // "devui-observability" JSON-RPC service registered above. Mirrors the Observability section
        // page (qwc-observability-section.js), which is likewise an internal core component under
        // this namespace.
        UnlistedPageBuildItem page = new UnlistedPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .internal()
                .namespace(NAMESPACE)
                .icon(ICON)
                .title(TITLE)
                .componentLink("qwc-metrics.js"));
        unlistedPages.produce(page);

        // Unlisted pages are still associated with the producing extension, so the page keeps a
        // non-null extensionId and is NOT "internal" for id purposes (isInternal() requires a null
        // extensionId). Dev UI therefore derives the page id as "<namespace>/<dashed-title>" =
        // "devui-observability/metrics". internal() above only changes where the component JS is
        // resolved from (the core qwc/ bucket), not the id. The Observability section navigates by
        // this id, so the signal's pageId must match it exactly.
        signals.produce(new ObservabilitySignalBuildItem(
                "metrics",
                TITLE,
                ICON,
                NAMESPACE + "/metrics",
                "meterCount"));
    }
}
