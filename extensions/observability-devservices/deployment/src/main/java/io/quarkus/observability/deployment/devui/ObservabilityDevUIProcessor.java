package io.quarkus.observability.deployment.devui;

import java.util.Map;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;

/**
 * Dev UI card for displaying important details such LGTM embedded UI.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class ObservabilityDevUIProcessor {

    private static final String FEATURE = Feature.OBSERVABILITY.getName();

    @BuildStep(onlyIf = IsDevelopment.class)
    void createVersion(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer,
            BuildProducer<FooterPageBuildItem> footerProducer) {

        final CardPageBuildItem card = new CardPageBuildItem();

        // Grafana
        card.addPage(Page.externalPageBuilder("Grafana UI")
                .dynamicUrlJsonRPCMethodName("devui-dev-services:devServicesConfig",
                        Map.of("name", FEATURE, "configKey", "grafana.endpoint"))
                .doNotEmbed()
                .isHtmlContent()
                .icon("font-awesome-solid:chart-line"));

        // Open Telemetry
        card.addPage(Page.externalPageBuilder("OpenTelemetry Port")
                .icon("font-awesome-solid:binoculars")
                .doNotEmbed()
                .dynamicUrlJsonRPCMethodName("devui-dev-services:devServicesConfig",
                        Map.of("name", FEATURE, "configKey", "otel-collector.url")));

        card.setCustomCard("qwc-lgtm-card.js");
        cardPageBuildItemBuildProducer.produce(card);

        // LGTM Container Log Console
        WebComponentPageBuilder logPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-solid:chart-line")
                .title("LGTM")
                .componentLink("qwc-lgtm-log.js");

        footerProducer.produce(new FooterPageBuildItem(logPageBuilder));
    }
}
