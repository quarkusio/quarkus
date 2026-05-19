package io.quarkus.flyway.mongodb.deployment.devui;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.flyway.mongodb.runtime.dev.ui.FlywayMongodbDevUIRecorder;
import io.quarkus.flyway.mongodb.runtime.dev.ui.FlywayMongodbJsonRpcService;

public class FlywayMongodbDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(value = RUNTIME_INIT, optional = true)
    CardPageBuildItem create(FlywayMongodbDevUIRecorder recorder) {
        recorder.initializeJsonRpcService();

        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-flyway-mongodb-clients.js")
                .dynamicLabelJsonRPCMethodName("getNumberOfClients")
                .icon("font-awesome-solid:database"));
        return card;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(FlywayMongodbJsonRpcService.class);
    }
}
