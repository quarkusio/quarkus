package io.quarkus.opentelemetry.deployment.devui;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;
import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.OTLP_VALUE;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.observability.store.metrics.PrometheusNamingId;
import io.quarkus.devui.spi.observability.MetricsBackendBuildItem;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.exporter.OtlpExporterBuildConfig;
import io.quarkus.opentelemetry.runtime.devui.DevUiMetricsSdkBuilderCustomizer;

/**
 * Wires native (and bridged) OpenTelemetry metrics into the Dev UI in dev mode by adding a
 * dev-only in-memory PeriodicMetricReader to the SDK meter provider. Active whenever OTel
 * metrics are enabled; combined with the Micrometer presence matrix this yields full
 * coverage with no double counting. The metrics view has no separate build-time enable flag;
 * it is dev-only via IsLocalDevelopment and never registered in prod/native.
 */
@BuildSteps(onlyIf = { OpenTelemetryEnabled.class, IsLocalDevelopment.class })
public class OpenTelemetryMetricsDevUIProcessor {

    @BuildStep
    void registerOtelMetricsCapture(OTelBuildConfig oTelBuildConfig,
            OtlpExporterBuildConfig otlpExporterBuildConfig,
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
        // Captured from the SDK. Only metrics exported over OTLP are named the way Prometheus renames OTLP on
        // receipt; with no exporter, or another one, how they reach Prometheus (if at all) is not known here.
        backends.produce(new MetricsBackendBuildItem("otel",
                exportedOverOtlp(oTelBuildConfig.metrics().exporter(), otlpExporterBuildConfig)
                        ? PrometheusNamingId.OTLP
                        : null));
    }

    /**
     * Whether the metrics leave over OTLP: through the Quarkus managed exporter ({@code cdi}, the default, as
     * long as it is not disabled), or through the upstream OTLP exporter ({@code otlp}).
     */
    private static boolean exportedOverOtlp(List<String> exporters, OtlpExporterBuildConfig otlpExporterBuildConfig) {
        return exporters.contains(OTLP_VALUE)
                || (exporters.contains(CDI_VALUE) && otlpExporterBuildConfig.enabled());
    }
}
