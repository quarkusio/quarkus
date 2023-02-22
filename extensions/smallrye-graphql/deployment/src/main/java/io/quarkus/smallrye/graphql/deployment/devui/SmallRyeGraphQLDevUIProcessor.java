package io.quarkus.smallrye.graphql.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfig;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class SmallRyeGraphQLDevUIProcessor {

    SmallRyeGraphQLConfig graphQLConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem createCard(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem("SmallRye GraphQL");

        // Generated GraphQL Schema
        PageBuilder schemaPage = Page.externalPageBuilder("GraphQL Schema")
                .icon("font-awesome-solid:diagram-project")
                .url("/" + graphQLConfig.rootPath + "/schema.graphql");

        // GraphiQL UI
        String uiPath = nonApplicationRootPathBuildItem.resolvePath(graphQLConfig.ui.rootPath);
        PageBuilder uiPage = Page.externalPageBuilder("GraphQL UI")
                .icon("font-awesome-solid:table-columns")
                .url(uiPath);

        cardPageBuildItem.addPage(schemaPage);
        cardPageBuildItem.addPage(uiPage);

        return cardPageBuildItem;
    }
}