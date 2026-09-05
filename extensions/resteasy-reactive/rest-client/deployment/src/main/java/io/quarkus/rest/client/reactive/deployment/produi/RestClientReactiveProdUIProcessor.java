package io.quarkus.rest.client.reactive.deployment.produi;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveInfoBuildItem;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.rest.client.reactive.runtime.produi.RestClientReactiveProdUIService;

/**
 * Contributes a read-only Prod UI page listing the configured REST clients and
 * their base URLs. The Dev UI is not reused: its {@code qwc-rest-client-clients}
 * component depends on dev-only web components and its backing container is
 * {@code @IfBuildProfile("dev")}. The steps consume
 * {@link JaxrsClientReactiveInfoBuildItem} so the page is only contributed when
 * REST clients are actually present.
 */
public class RestClientReactiveProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the client gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(RestClientReactiveProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI(JaxrsClientReactiveInfoBuildItem jaxrsClientReactiveInfoBuildItem) {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("REST Clients")
                .icon("font-awesome-solid:server")
                .componentLink("pwc-rest-client-clients.js"));
        return page;
    }
}
