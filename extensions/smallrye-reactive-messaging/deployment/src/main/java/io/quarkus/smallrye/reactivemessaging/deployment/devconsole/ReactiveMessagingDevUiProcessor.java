package io.quarkus.smallrye.reactivemessaging.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.ReactiveMessagingJsonRpcService;

public class ReactiveMessagingDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create() {
        CardPageBuildItem card = new CardPageBuildItem("SmallRye Reactive Messaging");
        card.addPage(Page.webComponentPageBuilder()
                .title("Channels")
                .componentLink("qwc-smallrye-reactive-messaging-channels.js")
                .icon("font-awesome-solid:diagram-project"));

        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem("SmallRyeReactiveMessaging",
                ReactiveMessagingJsonRpcService.class);
    }
}
