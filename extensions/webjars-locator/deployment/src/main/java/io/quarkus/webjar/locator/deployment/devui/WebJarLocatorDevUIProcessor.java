package io.quarkus.webjar.locator.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.webjar.locator.deployment.ImportMapBuildItem;

public class WebJarLocatorDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer,
            List<WebJarLibrariesBuildItem> webJarLibrariesBuildItems,
            Optional<ImportMapBuildItem> importMapBuildItem) {

        List<WebJarLibrary> webJarLibraries = new ArrayList<>();
        for (WebJarLibrariesBuildItem webJarLibrariesBuildItem : webJarLibrariesBuildItems) {
            webJarLibraries.addAll(webJarLibrariesBuildItem.getWebJarLibraries());
        }

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        if (!webJarLibraries.isEmpty()) {
            // WebJar Libraries
            cardPageBuildItem.addBuildTimeData("webJarLibraries", webJarLibraries);

            // WebJar Asset List
            cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                    .componentLink("qwc-webjar-locator-webjar-libraries.js")
                    .title("Web libraries")
                    .icon("font-awesome-solid:folder-tree")
                    .staticLabel(String.valueOf(webJarLibraries.size())));

            if (importMapBuildItem.isPresent()) {
                cardPageBuildItem.addBuildTimeData("importMap", importMapBuildItem.get().getImportMap());

                // ImportMap
                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .componentLink("qwc-webjar-locator-importmap.js")
                        .title("Import Map")
                        .icon("font-awesome-solid:diagram-project"));

            }

        }

        cardPageProducer.produce(cardPageBuildItem);
    }

}
