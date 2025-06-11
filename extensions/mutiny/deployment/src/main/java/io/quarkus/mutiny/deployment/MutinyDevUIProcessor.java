package io.quarkus.mutiny.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;

/**
 * Dev UI card for displaying important details such as the Caffeine library version.
 */
public class MutinyDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        card.addLibraryVersion("io.smallrye.reactive", "mutiny", "Mutiny",
                "https://smallrye.io/smallrye-mutiny/");

        card.setLogo("logo_dark.png", "logo_light.png");

        cardPageBuildItemBuildProducer.produce(card);
    }

}
