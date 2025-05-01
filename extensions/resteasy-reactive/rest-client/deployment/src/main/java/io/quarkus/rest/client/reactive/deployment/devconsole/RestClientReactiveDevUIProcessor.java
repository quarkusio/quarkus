package io.quarkus.rest.client.reactive.deployment.devconsole;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveInfoBuildItem;
import io.quarkus.rest.client.reactive.runtime.devui.RestClientsContainer;
import io.quarkus.rest.client.reactive.runtime.devui.RestClientsJsonRPCService;

public class RestClientReactiveDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(RestClientsContainer.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(JaxrsClientReactiveInfoBuildItem jaxrsClientReactiveInfoBuildItem) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("REST Clients")
                .componentLink("qwc-rest-client-clients.js")
                .icon("font-awesome-solid:server")
                .staticLabel(String.valueOf(jaxrsClientReactiveInfoBuildItem.getInterfaceNames().size())));

        return pageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(RestClientsJsonRPCService.class);
    }
}
