package io.quarkus.smallrye.openapi.deployment.devui;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.DevContextBuildItem;
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
            Optional<DevContextBuildItem> devContextBuildItem,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openApiConfig) {

        String devUIContextRoot;
        if (devContextBuildItem.isPresent()) {
            devUIContextRoot = devContextBuildItem.get().getDevUIContextRoot();
        } else {
            devUIContextRoot = "";
        }
        String uiPath = devUIContextRoot + nonApplicationRootPathBuildItem.resolveManagementPath(swaggerUiConfig.path(),
                managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

        Config c = ConfigProvider.getConfig();
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        if (c.getOptionalValue("quarkus.swagger-ui.enabled", Boolean.class).orElse(Boolean.TRUE)) {
            cardPageBuildItem.addPage(Page.externalPageBuilder("Swagger UI")
                    .url(uiPath + "/index.html?embed=true", uiPath)
                    .isHtmlContent()
                    .icon("font-awesome-solid:signs-post"));
        }

        cardPageBuildItem.addLibraryVersion("io.smallrye", "smallrye-open-api-jaxrs", "SmallRye OpenAPI",
                "https://github.com/smallrye/smallrye-open-api");

        cardPageBuildItem.addLibraryVersion("org.eclipse.microprofile.openapi", "microprofile-openapi-api",
                "MicroProfile OpenAPI", "https://github.com/microprofile/microprofile-open-api");
        cardPageBuildItem.setLogo("openapi_logo.png", "openapi_logo.png");

        openApiConfig.documents().forEach((documentName, documentConfig) -> {

            String schemaPath = devUIContextRoot
                    + nonApplicationRootPathBuildItem.resolveManagementPath(documentConfig.path(),
                            managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());

            String jsonSchema = schemaPath + "?format=json";
            if (SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME.equals(documentName)) {
                cardPageBuildItem.addPage(Page.externalPageBuilder("Schema yaml")
                        .url(schemaPath, schemaPath)
                        .isYamlContent()
                        .icon("font-awesome-solid:file-lines"));

                cardPageBuildItem.addPage(Page.externalPageBuilder("Schema json")
                        .url(jsonSchema, jsonSchema)
                        .isJsonContent()
                        .icon("font-awesome-solid:file-code"));
            } else {
                cardPageBuildItem.addPage(Page.externalPageBuilder("Schema yaml " + documentName)
                        .url(schemaPath, schemaPath)
                        .isYamlContent()
                        .icon("font-awesome-solid:file-lines"));

                cardPageBuildItem.addPage(Page.externalPageBuilder("Schema json " + documentName)
                        .url(jsonSchema, jsonSchema)
                        .isJsonContent()
                        .icon("font-awesome-solid:file-code"));
            }
        });

        cardPageBuildItem.addPage(Page.assistantPageBuilder()
                .title("Generate clients for default OpenAPI document")
                .componentLink("qwc-openapi-generate-client.js"));

        return cardPageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(OpenApiJsonRpcService.class);
    }

}
