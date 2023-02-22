package io.quarkus.smallrye.openapi.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class OpenApiDevUIProcessor {

    private static final String NAME = "Smallrye Openapi";

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem(NAME);

        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema yaml")
                .url(nonApplicationRootPathBuildItem.resolvePath("openapi"))
                .isYamlContent()
                .icon("font-awesome-solid:file-lines"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema json")
                .url(nonApplicationRootPathBuildItem.resolvePath("openapi") + "?format=json")
                .isJsonContent()
                .icon("font-awesome-solid:file-code"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Swagger UI")
                .url(nonApplicationRootPathBuildItem.resolvePath("swagger-ui"))
                .isHtmlContent()
                .icon("font-awesome-solid:signs-post"));

        return cardPageBuildItem;
    }

}
