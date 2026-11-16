package io.quarkus.observability.deployment.devui;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.observability.runtime.ObservabilityJsonRPCService;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;

/**
 * Dev UI card for displaying important details such LGTM embedded UI.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class,
        ObservabilityDevUIProcessor.IsLgtmDevUiEnabled.class })
public class ObservabilityDevUIProcessor {

    private static final List<String> EXPLICIT_ENDPOINT_PROPERTIES = List.of(
            "quarkus.otel.exporter.otlp.endpoint",
            "quarkus.otel.exporter.otlp.traces.endpoint",
            "quarkus.otel.exporter.otlp.metrics.endpoint",
            "quarkus.otel.exporter.otlp.logs.endpoint",
            "quarkus.micrometer.export.otlp.url");

    public static class IsLgtmDevUiEnabled implements BooleanSupplier {
        ObservabilityConfiguration config;

        @Override
        public boolean getAsBoolean() {
            if (!config.enabled() || config.devResources() || !config.lgtm().enabled()) {
                return false;
            }
            var cfg = ConfigProvider.getConfig();
            return EXPLICIT_ENDPOINT_PROPERTIES.stream()
                    .noneMatch(key -> cfg.getOptionalValue(key, String.class).isPresent());
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createVersion(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer,
            BuildProducer<FooterPageBuildItem> footerProducer) {

        final CardPageBuildItem card = new CardPageBuildItem();

        // Grafana
        card.addPage(Page.externalPageBuilder("Grafana UI")
                .icon("font-awesome-solid:chart-line")
                .doNotEmbed()
                .dynamicUrlJsonRPCMethodName("getGrafanaEndpoint"));

        // Open Telemetry
        card.addPage(Page.externalPageBuilder("OpenTelemetry Port")
                .icon("font-awesome-solid:binoculars")
                .doNotEmbed()
                .url("https://opentelemetry.io/")
                .dynamicLabelJsonRPCMethodName("getOtelEndpoint"));

        cardPageBuildItemBuildProducer.produce(card);

        // LGTM Container Log Console
        WebComponentPageBuilder logPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-solid:chart-line")
                .title("LGTM")
                .componentLink("qwc-lgtm-log.js");

        footerProducer.produce(new FooterPageBuildItem(logPageBuilder));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(ObservabilityJsonRPCService.class, BuiltinScope.SINGLETON.getName());
    }
}
