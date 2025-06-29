package io.quarkus.liquibase.deployment.devui;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.liquibase.runtime.dev.ui.LiquibaseJsonRpcService;

/**
 * Dev UI card for displaying important details such as the library version.
 */
public class LiquibaseDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        // card
        card.setLogo("liquibase_logo.svg", "liquibase_logo.svg");
        card.addLibraryVersion("org.liquibase", "liquibase-core", "Liquibase", "https://www.liquibase.com/");

        card.addPage(Page.webComponentPageBuilder().title("Datasources")
                .componentLink("qwc-liquibase-datasources.js")
                .icon("font-awesome-solid:database")
                .dynamicLabelJsonRPCMethodName("getDatasourceCount"));

        cardPageBuildItemBuildProducer.produce(card);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(LiquibaseJsonRpcService.class);
    }
}
