package io.quarkus.agroal.deployment.produi;

import io.quarkus.agroal.runtime.produi.AgroalPoolProdUIService;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only JDBC connection pool view to the Prod UI. Unlike the
 * Dev UI database inspector (which executes SQL), this exposes only pool
 * metrics, so it is safe to serve in production.
 */
class AgroalProdUIProcessor {

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(AgroalPoolProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Connection pools")
                .icon("font-awesome-solid:database")
                .componentLink("pwc-agroal-pool.js"));
        return page;
    }
}
