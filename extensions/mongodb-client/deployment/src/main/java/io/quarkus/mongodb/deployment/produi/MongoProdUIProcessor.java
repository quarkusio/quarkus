package io.quarkus.mongodb.deployment.produi;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.mongodb.deployment.MongoClientBuildItem;
import io.quarkus.mongodb.runtime.produi.MongoProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page listing the configured MongoDB clients
 * with their connection/pool configuration and a live ping result. There is no
 * Dev UI data page to reuse (the MongoDB Dev UI is only a Dev Services console
 * link), so a bespoke read-only component + service is provided. The service
 * issues only the {@code ping} command and never exposes credentials. The page
 * is only added when at least one MongoDB client has been created.
 */
public class MongoProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the client gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(MongoProdUIService.class);
    }

    @BuildStep
    void createProdUIPage(List<MongoClientBuildItem> clients,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        if (clients.isEmpty()) {
            return;
        }

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("MongoDB")
                .icon("font-awesome-solid:leaf")
                .componentLink("pwc-mongodb-clients.js"));
        prodUIProducer.produce(page);
    }
}
