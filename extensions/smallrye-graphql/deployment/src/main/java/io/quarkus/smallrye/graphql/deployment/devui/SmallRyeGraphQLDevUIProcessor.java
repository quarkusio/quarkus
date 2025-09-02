package io.quarkus.smallrye.graphql.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfig;
import io.quarkus.smallrye.graphql.runtime.dev.GraphQLJsonRpcService;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class SmallRyeGraphQLDevUIProcessor {

    SmallRyeGraphQLConfig graphQLConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem createCard(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addLibraryVersion("io.smallrye", "smallrye-graphql-cdi", "SmallRye GraphQL",
                "https://smallrye.io/smallrye-graphql");
        cardPageBuildItem.addLibraryVersion("org.eclipse.microprofile.graphql", "microprofile-graphql-api",
                "MicroProfile GraphQL",
                "https://github.com/microprofile/microprofile-graphql/");
        cardPageBuildItem.addLibraryVersion("com.graphql-java", "graphql-java", "GraphQL Java",
                "https://www.graphql-java.com/");

        cardPageBuildItem.setLogo("logo_dark.svg", "logo_light.svg");

        // Generated GraphQL Schema
        String schemaPath = "/" + graphQLConfig.rootPath() + "/schema.graphql";
        PageBuilder schemaPage = Page.externalPageBuilder("GraphQL Schema")
                .icon("font-awesome-solid:diagram-project")
                .url(schemaPath, schemaPath);

        // GraphiQL UI
        String uiPath = nonApplicationRootPathBuildItem.resolvePath(graphQLConfig.ui().rootPath());
        PageBuilder uiPage = Page.externalPageBuilder("GraphQL UI")
                .icon("font-awesome-solid:table-columns")
                .url(uiPath + "/index.html?embed=true", uiPath);

        // Learn
        PageBuilder learnLink = Page.externalPageBuilder("Learn more about GraphQL")
                .icon("font-awesome-solid:graduation-cap")
                .doNotEmbed()
                .url("https://graphql.org/");

        PageBuilder assistantPage = Page.assistantPageBuilder()
                .title("Generate clients")
                .componentLink("qwc-graphql-generate-client.js");

        cardPageBuildItem.addPage(uiPage);
        cardPageBuildItem.addPage(schemaPage);
        cardPageBuildItem.addPage(learnLink);
        cardPageBuildItem.addPage(assistantPage);

        return cardPageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(GraphQLJsonRpcService.class);
    }
}