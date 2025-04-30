package io.quarkus.kafka.streams.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.kafka.streams.runtime.devui.KafkaStreamsJsonRPCService;

public class KafkaStreamsDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-kafka-streams-topology.js")
                .title("Topology")
                .icon("font-awesome-solid:diagram-project"));

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep
    public void createJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCServiceProducer) {
        jsonRPCServiceProducer.produce(new JsonRPCProvidersBuildItem(KafkaStreamsJsonRPCService.class));
    }
}