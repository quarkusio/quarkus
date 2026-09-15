package io.quarkus.micrometer.deployment.produi;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.micrometer.runtime.produi.MicrometerProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only metrics browser to the Prod UI. The Dev UI only links
 * to the scrape endpoints, so a bespoke page is provided that lists the
 * registered meters and their current values.
 */
class MicrometerProdUIProcessor {

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(MicrometerProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        // The graphs page comes first (every meter charted); the raw table is the companion lookup.
        page.addPage(Page.webComponentPageBuilder()
                .title("Metrics")
                .icon("font-awesome-solid:chart-line")
                .componentLink("pwc-micrometer-metrics.js"));
        page.addPage(Page.webComponentPageBuilder()
                .title("Raw data")
                .icon("font-awesome-solid:table")
                .componentLink("pwc-micrometer-raw.js"));
        return page;
    }
}
