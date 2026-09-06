package io.quarkus.grpc.deployment.produi;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.grpc.deployment.BindableServiceBuildItem;
import io.quarkus.grpc.runtime.produi.GrpcProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page for the gRPC server. The Dev UI component
 * is not reused: it is primarily an invoke / test client (send messages, stream
 * calls) and imports dev-only modules, so a bespoke read-only component listing
 * the registered services and their methods is provided instead.
 */
public class GrpcProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the service gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(GrpcProdUIService.class);
    }

    @BuildStep
    void createProdUI(List<BindableServiceBuildItem> bindables,
            BuildProducer<ProdUIPageBuildItem> prodUIPages) {
        if (bindables.isEmpty()) {
            return;
        }

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("gRPC Services")
                .icon("font-awesome-solid:gears")
                .componentLink("pwc-grpc-services.js"));
        prodUIPages.produce(page);
    }
}
