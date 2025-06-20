package io.quarkus.smallrye.openapi.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.runtime.dev.OpenApiJsonRpcService;
import io.quarkus.swaggerui.deployment.SwaggerUiConfig;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

public class OpenApiDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openApiConfig) {

        String uiPath = nonApplicationRootPathBuildItem.resolveManagementPath(swaggerUiConfig.path(),
                managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

        String schemaPath = nonApplicationRootPathBuildItem.resolveManagementPath(openApiConfig.path(),
                managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addLibraryVersion("io.smallrye", "smallrye-open-api-jaxrs", "SmallRye OpenAPI",
                "https://github.com/smallrye/smallrye-open-api");
        cardPageBuildItem.addLibraryVersion("org.eclipse.microprofile.openapi", "microprofile-openapi-api",
                "MicroProfile OpenAPI", "https://github.com/microprofile/microprofile-open-api");
        cardPageBuildItem.setLogo("openapi_logo.png", "openapi_logo.png");

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

        cardPageBuildItem.addPage(Page.assistantPageBuilder()
                .title("Generate clients")
                .componentLink("qwc-openapi-generate-client.js"));

        return cardPageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(OpenApiJsonRpcService.class);
    }

}
