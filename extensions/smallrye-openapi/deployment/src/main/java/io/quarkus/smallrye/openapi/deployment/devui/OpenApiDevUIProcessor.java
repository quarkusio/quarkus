package io.quarkus.smallrye.openapi.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class OpenApiDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        String uiPath = nonApplicationRootPathBuildItem.resolvePath("swagger-ui");

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema yaml")
                .url(nonApplicationRootPathBuildItem.resolvePath("openapi"))
                .isYamlContent()
                .icon("font-awesome-solid:file-lines"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema json")
                .url(nonApplicationRootPathBuildItem.resolvePath("openapi") + "?format=json")
                .isJsonContent()
                .icon("font-awesome-solid:file-code"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Swagger UI")
                .url(uiPath + "/index.html?embed=true")
                .staticLabel("<a style='color: var(--lumo-contrast-80pct);' href='" + uiPath
                        + "' target='_blank'><vaadin-icon class='icon' icon='font-awesome-solid:up-right-from-square'></vaadin-icon></a>")
                .isHtmlContent()
                .icon("font-awesome-solid:signs-post"));

        return cardPageBuildItem;
    }

}
