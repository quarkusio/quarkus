package io.quarkus.vertx.deployment.produi;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.vertx.core.runtime.produi.VertxProdUIService;

/**
 * Contributes a read-only Prod UI page showing the Vert.x event-loop and worker
 * pool configuration and status. There is no Dev UI data page to reuse (the
 * Vert.x Dev UI ships only i18n bundles), so a bespoke read-only component +
 * service is provided. The service reads only the always-present Vertx bean and
 * configuration and issues no mutation; none of the exposed values are secrets.
 * The Vertx bean is always present when the extension is in use, so the page is
 * contributed unconditionally.
 */
public class VertxProdUIProcessor {

    @BuildStep
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {

        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(VertxProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Vert.x")
                .icon("font-awesome-solid:gauge-high")
                .componentLink("pwc-vertx.js"));
        prodUIProducer.produce(page);
    }
}
