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

/**
 * Registers the unified Dev UI metrics store producer + JSON-RPC service + Observability
 * signal when at least one backend adapter contributed a {@link MetricsBackendBuildItem}. One
 * signal regardless of how many backends are active (natural dedup). Backends produce
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
        // JSON-RPC UNDER THE EXPLICIT "devui-observability" NAMESPACE — must match the dashboard
        // page's namespace (so the page's JsonRpc client and the tests' super("devui-observability")
        // resolve). The service itself carries no scope annotation.
        jsonRpcProviders.produce(new JsonRPCProvidersBuildItem(NAMESPACE, MetricsDevUIJsonRPCService.class));

        // Metrics has no page of its own: the Observability dashboard renders one card per meter
        // the user picked, driving capture through this service's setSelection. The signal carries
        // no pageId and exists purely to tell the dashboard that metrics are available here.
        signals.produce(new ObservabilitySignalBuildItem(
                "metrics",
                TITLE,
                ICON,
                null,
                "meterCount"));
    }
}
