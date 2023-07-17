package io.quarkus.resteasy.reactive.server.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.resteasy.reactive.server.runtime.devui.ResteasyReactiveJsonRPCService;

public class ResteasyReactiveDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        // Endpoint Scores
        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-resteasy-reactive-endpoint-scores.js")
                .title("Endpoint scores")
                .icon("font-awesome-solid:chart-bar"));

        // Exception mappers
        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-resteasy-reactive-exception-mappers.js")
                .title("Exception Mappers")
                .icon("font-awesome-solid:bomb"));

        // Parameter converter providers
        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-resteasy-reactive-parameter-converter-providers.js")
                .title("Parameter converter providers")
                .icon("font-awesome-solid:arrow-right-arrow-left"));

        // Custom Card
        // For now, we don't display the score as it might be confusing for people using blocking
        //cardPageBuildItem.setCustomCard("qwc-resteasy-reactive-card.js");

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCServiceProducer) {
        jsonRPCServiceProducer.produce(new JsonRPCProvidersBuildItem(ResteasyReactiveJsonRPCService.class));
    }
}
