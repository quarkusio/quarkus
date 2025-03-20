package io.quarkus.smallrye.openapi.deployment.devui;

import java.util.Optional;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.assistant.AIBuildItem;
import io.quarkus.deployment.dev.assistant.AIClient;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.spi.OpenApiDocumentBuildItem;
import io.quarkus.swaggerui.deployment.SwaggerUiConfig;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

public class OpenApiDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openApiConfig,
            Optional<AIBuildItem> aIBuildItem) {

        String uiPath = nonApplicationRootPathBuildItem.resolveManagementPath(swaggerUiConfig.path(),
                managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

        String schemaPath = nonApplicationRootPathBuildItem.resolveManagementPath(openApiConfig.path(),
                managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.externalPageBuilder("Swagger UI")
                .url(uiPath + "/index.html?embed=true", uiPath)
                .isHtmlContent()
                .icon("font-awesome-solid:signs-post"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema yaml")
                .url(schemaPath, schemaPath)
                .isYamlContent()
                .icon("font-awesome-solid:file-lines"));

        String jsonSchema = schemaPath + "?format=json";
        cardPageBuildItem.addPage(Page.externalPageBuilder("Schema json")
                .url(jsonSchema, jsonSchema)
                .isJsonContent()
                .icon("font-awesome-solid:file-code"));

        if (aIBuildItem.isPresent()) {
            cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                    .title("Generate client")
                    .icon("font-awesome-solid:wand-magic-sparkles")
                    .componentLink("qwc-generate-client.js"));
        }

        return cardPageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createBuildTimeActions(Optional<AIBuildItem> aIBuildItem,
            OpenApiDocumentBuildItem apiDocumentBuildItem,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {

        if (aIBuildItem.isPresent()) {
            BuildTimeActionBuildItem buildTimeActionBuildItem = new BuildTimeActionBuildItem();

            buildTimeActionBuildItem.addAction("generateClient", params -> {
                params.put("schemaDocument", apiDocumentBuildItem.getSmallRyeOpenAPI().toJSON());
                AIClient aiClient = aIBuildItem.get().getAIClient();
                return aiClient.dynamic(USER_MESSAGE, params);
            });

            buildTimeActionProducer.produce(buildTimeActionBuildItem);
        }
    }

    private static final String USER_MESSAGE = """
            Given the OpenAPI Schema document :

            {{schemaDocument}}

            Please generate a {{language}} Object that act as a client to all the operations in the scheme.

            This {{language}} code must be able to be called like this (pseudo code):

            var stub = new ResourceNameHereClient();
            var response = stub.doOperation(someparam);

            The reponse object should only contain one field called `code` that contains a value with only the {{language}} code, nothing else, no explanation, and do not put the code in backticks.
            The {{language}} code must run and be valid.

            Example: {code: 'package foo.bar; // mode code here'}

            {{extraContext}}

            """;

}
