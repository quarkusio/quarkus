package io.quarkus.caffeine.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;

/**
 * Dev UI card for displaying important details such as the Caffeine library version.
 */
public class CaffeineDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        card.addLibraryVersion("com.github.ben-manes.caffeine", "caffeine", "Caffeine",
                "https://github.com/ben-manes/caffeine");

        card.setLogo("logo_dark.png", "logo_light.png");

        cardPageBuildItemBuildProducer.produce(card);
    }

}
