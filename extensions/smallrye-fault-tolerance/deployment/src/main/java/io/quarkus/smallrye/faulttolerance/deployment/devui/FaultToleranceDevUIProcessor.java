package io.quarkus.smallrye.faulttolerance.deployment.devui;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.devui.FaultToleranceJsonRpcService;
import io.quarkus.smallrye.faulttolerance.runtime.produi.FaultToleranceProdUIService;

public class FaultToleranceDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem cardPage(FaultToleranceInfoBuildItem faultToleranceInfo) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Guarded Methods")
                .icon("font-awesome-solid:life-ring")
                .componentLink("qwc-fault-tolerance-methods.js")
                .staticLabel("" + faultToleranceInfo.getGuardedMethods()));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem jsonRPCService() {
        return new JsonRPCProvidersBuildItem(FaultToleranceJsonRpcService.class);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(FaultToleranceProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Guarded Methods")
                .icon("font-awesome-solid:life-ring")
                .componentLink("qwc-fault-tolerance-methods.js"));
        return page;
    }
}
