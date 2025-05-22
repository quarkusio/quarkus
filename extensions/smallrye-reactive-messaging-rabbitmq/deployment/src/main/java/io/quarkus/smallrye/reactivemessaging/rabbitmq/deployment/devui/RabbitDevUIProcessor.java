package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.dev.ui.RabbitHttpPortFinder;
import io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.dev.ui.RabbitMqJsonRpcService;

public class RabbitDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(RabbitHttpPortFinder.class);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();
        card.setCustomCard("qwc-rabbitmq-card.js");
        cardPageBuildItemBuildProducer.produce(card);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(RabbitMqJsonRpcService.class);
    }
}
