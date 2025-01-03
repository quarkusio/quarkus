package io.quarkus.cache.deployment.devui;

import io.quarkus.cache.runtime.dev.ui.CacheJsonRPCService;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

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
}
