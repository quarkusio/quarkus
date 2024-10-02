package io.quarkus.devui.deployment.menu;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.devmode.ResourceNotFoundData;

/**
 * This creates Endpoints Page
 */
public class EndpointsProcessor {
    private static final String NAMESPACE = "devui-endpoints";
    private static final String DEVUI = "dev-ui";

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createEndpointsPage(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        String basepath = nonApplicationRootPathBuildItem.resolvePath(DEVUI);

        InternalPageBuildItem endpointsPage = new InternalPageBuildItem("Endpoints", 25);

        endpointsPage.addBuildTimeData("basepath", basepath);

        // Page
        endpointsPage.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("qwc-endpoints.js"));

        endpointsPage.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Routes")
                .icon("font-awesome-solid:route")
                .componentLink("qwc-routes.js"));

        return endpointsPage;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(NAMESPACE, ResourceNotFoundData.class);
    }
}
