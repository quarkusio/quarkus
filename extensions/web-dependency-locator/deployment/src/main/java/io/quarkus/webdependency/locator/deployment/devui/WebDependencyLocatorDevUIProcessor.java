package io.quarkus.webdependency.locator.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.webdependency.locator.deployment.ImportMapBuildItem;

public class WebDependencyLocatorDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void createPages(BuildProducer<CardPageBuildItem> cardPageProducer,
            List<WebDependencyLibrariesBuildItem> webDependencyLibrariesBuildItems,
            Optional<ImportMapBuildItem> importMapBuildItem) {

        List<WebDependencyLibrary> webDependencyLibraries = new ArrayList<>();
        for (WebDependencyLibrariesBuildItem webDependencyLibrariesBuildItem : webDependencyLibrariesBuildItems) {
            webDependencyLibraries.addAll(webDependencyLibrariesBuildItem.getWebDependencyLibraries());
        }

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("javascript_logo.svg", "javascript_logo.svg");
        if (!webDependencyLibraries.isEmpty()) {
            // Web Dependency Libraries
            cardPageBuildItem.addBuildTimeData("webDependencyLibraries", webDependencyLibraries);

            // Web Dependency Asset List
            cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                    .componentLink("qwc-web-dependency-locator-libraries.js")
                    .title("Web libraries")
                    .icon("font-awesome-solid:folder-tree")
                    .staticLabel(String.valueOf(webDependencyLibraries.size())));

            if (importMapBuildItem.isPresent()) {
                cardPageBuildItem.addBuildTimeData("importMap", importMapBuildItem.get().getImportMap());

                // ImportMap
                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .componentLink("qwc-web-dependency-locator-importmap.js")
                        .title("Import Map")
                        .icon("font-awesome-solid:diagram-project"));

            }

        }

        cardPageProducer.produce(cardPageBuildItem);
    }

}
