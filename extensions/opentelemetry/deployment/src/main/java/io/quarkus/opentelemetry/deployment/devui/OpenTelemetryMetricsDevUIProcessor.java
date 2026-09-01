package io.quarkus.opentelemetry.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.devui.DevUiMetricsSdkBuilderCustomizer;

/**
 * Wires native (and bridged) OpenTelemetry metrics into the Dev UI in dev mode by adding a
 * dev-only in-memory PeriodicMetricReader to the SDK meter provider. Active whenever OTel
 * metrics are enabled; combined with the Micrometer presence matrix this yields full
 * coverage with no double counting. The metrics view has no separate build-time enable flag;
 * it is dev-only via IsLocalDevelopment and never registered in prod/native.
 */
public class OpenTelemetryMetricsDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerOtelMetricsCapture(OTelBuildConfig oTelBuildConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<MetricsBackendBuildItem> backends) {
        if (!oTelBuildConfig.metrics().enabled().orElse(Boolean.TRUE)) {
            return;
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(DevUiMetricsSdkBuilderCustomizer.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
        backends.produce(new MetricsBackendBuildItem("otel"));
    }
}
