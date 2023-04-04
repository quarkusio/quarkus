package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment.devconsole;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.devconsole.DevRabbitMqHttpPortSupplier;
import io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.devconsole.RabbitHttpPortFinder;
import io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.devconsole.RabbitMqJsonRpcService;

public class RabbitDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectInfos(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("rabbitHttpPort",
                new DevRabbitMqHttpPortSupplier(), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(RabbitHttpPortFinder.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();
        card.setCustomCard("qwc-rabbitmq-card.js");
        cardPageBuildItemBuildProducer.produce(card);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(RabbitMqJsonRpcService.class);
    }
}
