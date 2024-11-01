package io.quarkus.kafka.streams.deployment.devui;

import java.util.Set;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevModeCleanupBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.kafka.streams.runtime.devmode.KafkaStreamsHotReplacementSetup;
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

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCServiceProducer) {
        jsonRPCServiceProducer.produce(new JsonRPCProvidersBuildItem(KafkaStreamsJsonRPCService.class));
    }

    @BuildStep(onlyIf = IsNormal.class)
    void cleanProd(BuildProducer<DevModeCleanupBuildItem> producer) {
        producer.produce(new DevModeCleanupBuildItem(
                Set.of(KafkaStreamsHotReplacementSetup.class, KafkaStreamsJsonRPCService.class), true));
    }
}
