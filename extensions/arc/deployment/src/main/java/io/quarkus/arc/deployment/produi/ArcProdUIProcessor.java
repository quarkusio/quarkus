package io.quarkus.arc.deployment.produi;

import io.quarkus.arc.runtime.produi.ArcProdUIService;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page for the running ArC (CDI) container: the
 * registered beans, observer methods and interceptors plus the supported
 * scopes. The Dev UI is not reused - its {@code qwc-arc-*} components are backed
 * by build-time data and import dev-only web modules that the Prod UI bundle
 * does not shim, and it carries dev-only monitoring (fired events, invocation
 * trees). A bespoke read-only component is provided instead, backed by a runtime
 * service that reads the always-present {@code ArcContainerImpl}. Only bean
 * metadata is exposed; nothing is created, mutated or destroyed.
 */
public class ArcProdUIProcessor {

    @BuildStep
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(ArcProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("CDI")
                .icon("font-awesome-solid:egg")
                .componentLink("pwc-arc-container.js"));
        prodUIProducer.produce(page);
    }
}
