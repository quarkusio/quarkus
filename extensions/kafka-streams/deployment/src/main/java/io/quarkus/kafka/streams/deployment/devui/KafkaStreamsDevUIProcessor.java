package io.quarkus.kafka.streams.deployment.devui;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.kafka.streams.runtime.dev.ui.KafkaStreamsJsonRPCService;

public class KafkaStreamsDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("kafka_dark.png", "kafka_light.png");
        cardPageBuildItem.addLibraryVersion("org.apache.kafka", "kafka-streams", "Apache Kafka", "https://kafka.apache.org/");

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-kafka-streams-topology.js")
                .title("Topology")
                .icon("font-awesome-solid:diagram-project"));

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public void createJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCServiceProducer) {
        jsonRPCServiceProducer.produce(new JsonRPCProvidersBuildItem(KafkaStreamsJsonRPCService.class));
    }
}
