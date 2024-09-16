package io.quarkus.observability.deployment.devui;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;

/**
 * Dev UI card for displaying important details such LGTM embedded UI.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
public class ObservabilityDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createVersion(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer,
            BuildProducer<FooterPageBuildItem> footerProducer,
            Optional<ObservabilityDevServicesConfigBuildItem> configProps) {

        // LGTM
        if (configProps.isPresent()) {
            Map<String, String> runtimeConfig = configProps.get().getConfig();
            String grafanaUrl = runtimeConfig.getOrDefault("grafana.endpoint", "");

            if (StringUtils.isNotEmpty(grafanaUrl)) {
                final CardPageBuildItem card = new CardPageBuildItem();

                // Grafana
                grafanaUrl = StringUtils.prependIfMissing(grafanaUrl, "http://");
                card.addPage(Page.externalPageBuilder("Grafana UI")
                        .url(grafanaUrl, grafanaUrl)
                        .doNotEmbed()
                        .isHtmlContent()
                        .icon("font-awesome-solid:chart-line"));

                // Open Telemetry
                final ExternalPageBuilder otelPage = Page.externalPageBuilder("OpenTelemetry Port")
                        .icon("font-awesome-solid:binoculars")
                        .doNotEmbed()
                        .url("https://opentelemetry.io/")
                        .staticLabel(StringUtils
                                .substringAfterLast(runtimeConfig.getOrDefault("otel-collector.url", "0"), ":"));
                card.addPage(otelPage);

                card.setCustomCard("qwc-lgtm-card.js");
                cardPageBuildItemBuildProducer.produce(card);

                // LGTM Container Log Console
                WebComponentPageBuilder mailLogPageBuilder = Page.webComponentPageBuilder()
                        .icon("font-awesome-solid:chart-line")
                        .title("LGTM")
                        .componentLink("qwc-lgtm-log.js");

                footerProducer.produce(new FooterPageBuildItem(mailLogPageBuilder));
            }
        }

    }
}