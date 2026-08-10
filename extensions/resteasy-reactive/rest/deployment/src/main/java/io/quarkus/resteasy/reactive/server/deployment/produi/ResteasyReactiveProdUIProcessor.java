package io.quarkus.resteasy.reactive.server.deployment.produi;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDeploymentBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.produi.ResteasyReactiveProdUIService;

/**
 * Contributes a read-only Prod UI page for Quarkus REST (RESTEasy Reactive)
 * endpoints. The Dev UI is not reused: its Endpoints page renders the core
 * dev-only {@code qwc-endpoints} component (backed by build-time data) and its
 * other pages show dev-only endpoint scoring. A bespoke read-only component
 * listing the endpoint metadata is provided instead. The steps consume
 * {@link ResteasyReactiveDeploymentBuildItem} so the page is only contributed
 * when REST endpoints are actually present.
 */
public class ResteasyReactiveProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the deployment gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(ResteasyReactiveProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI(ResteasyReactiveDeploymentBuildItem deployment) {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("pwc-resteasy-reactive-endpoints.js"));
        return page;
    }
}
