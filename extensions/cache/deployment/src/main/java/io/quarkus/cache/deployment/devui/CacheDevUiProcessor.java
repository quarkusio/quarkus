package io.quarkus.cache.deployment.devui;

import io.quarkus.cache.runtime.dev.ui.CacheJsonRPCService;
import io.quarkus.cache.runtime.produi.CacheProdUIService;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

public class CacheDevUiProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem create(CurateOutcomeBuildItem bi) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Caches")
                .componentLink("qwc-cache-caches.js")
                .icon("font-awesome-solid:database"));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CacheJsonRPCService.class);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CacheProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Caches")
                .componentLink("qwc-cache-caches.js")
                .icon("font-awesome-solid:database")
                // Live, read-only badge next to the link: the current number of caches.
                .dynamicLabelJsonRPCMethodName("count"));
        return page;
    }
}
