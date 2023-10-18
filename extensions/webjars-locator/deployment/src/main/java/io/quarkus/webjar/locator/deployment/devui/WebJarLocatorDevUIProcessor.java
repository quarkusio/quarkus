package io.quarkus.webjar.locator.deployment.devui;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class WebJarLocatorDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer,
            WebJarLibrariesBuildItem webJarLibrariesBuildItem) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        List<WebJarLibrary> webJarLibraries = webJarLibrariesBuildItem.getWebJarLibraries();

        if (!webJarLibraries.isEmpty()) {
            // WebJar Libraries
            cardPageBuildItem.addBuildTimeData("webJarLibraries", webJarLibraries);

            // WebJar Asset List
            cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                    .componentLink("qwc-webjar-locator-webjar-libraries.js")
                    .title("WebJar Libraries")
                    .icon("font-awesome-solid:folder-tree")
                    .staticLabel(String.valueOf(webJarLibraries.size())));
        }

        cardPageProducer.produce(cardPageBuildItem);
    }

}
