package io.quarkus.smallrye.openapi.deployment.produi;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.deployment.DocumentFiltersBuildItem;
import io.quarkus.smallrye.openapi.runtime.produi.SmallRyeOpenApiProdUIService;
import io.quarkus.swaggerui.deployment.SwaggerUiConfig;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * Contributes a read-only Prod UI page for SmallRye OpenAPI. The Dev UI card is
 * not reused: it links to the embedded Swagger UI, the raw schema endpoints and
 * an assistant-only client generator. A bespoke read-only component that shows
 * the OpenAPI schema and derives the operation list from it is provided instead.
 * The steps consume {@link DocumentFiltersBuildItem} so the page is only
 * contributed when the OpenAPI extension is active.
 *
 * When Swagger UI is also served in production (only the case when
 * {@code quarkus.swagger-ui.always-include=true}), an external link to it is added
 * so operators can reach the interactive UI from the OpenAPI card.
 */
public class SmallRyeOpenApiProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the OpenAPI gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(SmallRyeOpenApiProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI(DocumentFiltersBuildItem documentFiltersBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openApiConfig) {

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("OpenAPI Schema")
                .icon("font-awesome-solid:file-code")
                .componentLink("pwc-openapi-schema.js"));

        // Swagger UI is only served in production when always-include is set (mirrors
        // SwaggerUiProcessor.shouldInclude). When it is, surface a link to it; the target
        // is resolved the same way the Dev UI card does so it points at the right interface.
        Config config = ConfigProvider.getConfig();
        boolean swaggerUiEnabled = config.getOptionalValue("quarkus.swagger-ui.enabled", Boolean.class).orElse(Boolean.TRUE);
        boolean included = launchModeBuildItem.getLaunchMode().isDevOrTest() || swaggerUiConfig.alwaysInclude();
        if (swaggerUiEnabled && included) {
            String uiPath = nonApplicationRootPathBuildItem.resolveManagementPath(swaggerUiConfig.path(),
                    managementBuildTimeConfig, launchModeBuildItem, openApiConfig.managementEnabled());
            page.addPage(Page.externalPageBuilder("Swagger UI")
                    .url(uiPath)
                    .icon("font-awesome-solid:signs-post")
                    .doNotEmbed(true));
        }

        return page;
    }
}
