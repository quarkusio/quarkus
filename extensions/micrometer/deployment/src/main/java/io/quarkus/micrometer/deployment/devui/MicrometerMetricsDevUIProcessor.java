package io.quarkus.micrometer.deployment.devui;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.runtime.devui.DevUiMetricsSampler;

/**
 * Wires the Micrometer metrics capture into the Dev UI in dev mode. Presence matrix: the
 * Micrometer sampler is suppressed when the micrometer->OpenTelemetry bridge is present AND
 * OTel metrics are enabled, because the bridge then forwards Micrometer meters into the OTel
 * SDK, which the OTel reader already captures — avoiding double counting. If the bridge is
 * present but OTel metrics are disabled, the sampler runs as a fallback (the bridge's OTel
 * sink is a no-op, so nothing else would capture the meters).
 */
public class MicrometerMetricsDevUIProcessor {

    // Presence of the bridge runtime recorder means the micrometer->OTel bridge is active.
    private static final String BRIDGE_RECORDER = "io.quarkus.micrometer.opentelemetry.runtime.MicrometerOtelBridgeRecorder";

    @BuildStep(onlyIf = { IsLocalDevelopment.class, MicrometerProcessor.MicrometerEnabled.class })
    void registerMicrometerMetricsCapture(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<MetricsBackendBuildItem> backends) {
        // The Dev UI metrics view is active whenever a metrics backend and the Dev UI are present
        // in dev mode — there is no separate build-time enable flag. It is dev-only via
        // IsLocalDevelopment and never registered in prod/native.
        //
        // Presence matrix: suppress the Micrometer sampler ONLY when the bridge is present AND
        // OTel metrics are enabled — then the OTel in-memory reader already captures the bridged
        // Micrometer meters. If the bridge is present but OTel metrics are disabled, its OTel sink
        // is a no-op and no OTel reader is registered, so the sampler must run as a fallback
        // (otherwise nothing would be captured). Read the OTel flag via ConfigProvider to avoid a
        // hard compile dependency on the (optional) OpenTelemetry config classes.
        boolean bridgePresent = QuarkusClassLoader.isClassPresentAtRuntime(BRIDGE_RECORDER);
        boolean otelMetricsEnabled = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.otel.metrics.enabled", Boolean.class).orElse(Boolean.TRUE);
        if (bridgePresent && otelMetricsEnabled) {
            return; // covered by the OTel reader; avoid double counting
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(DevUiMetricsSampler.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
        backends.produce(new MetricsBackendBuildItem("micrometer"));
    }
}
