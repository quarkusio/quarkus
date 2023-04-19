package io.quarkus.rest.client.reactive.deployment.devconsole;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.rest.client.reactive.runtime.devconsole.RestClientsContainer;
import io.quarkus.rest.client.reactive.runtime.devconsole.RestClientsJsonRPCService;

public class RestClientReactiveDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem devConsoleInfo(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("devRestClients",
                new BeanLookupSupplier(RestClientsContainer.class), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(RestClientsContainer.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create() {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("REST Clients")
                .componentLink("qwc-rest-client-clients.js")
                .icon("font-awesome-solid:server"));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(RestClientsJsonRPCService.class);
    }
}
