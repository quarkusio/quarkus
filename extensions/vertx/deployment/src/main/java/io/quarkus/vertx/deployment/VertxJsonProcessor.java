package io.quarkus.vertx.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.vertx.core.spi.JsonFactory;

public class VertxJsonProcessor {

    @BuildStep
    void nativeSupport(List<ReinitializeVertxJsonBuildItem> reinitializeVertxJson,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassProducer,
            BuildProducer<ServiceProviderBuildItem> serviceProviderBuildItemBuildProducer) {
        if (reinitializeVertxJson.isEmpty()) {
            return;
        }
        runtimeReinitializedClassProducer
                .produce(new RuntimeReinitializedClassBuildItem(io.vertx.core.json.Json.class.getName()));
        runtimeReinitializedClassProducer
                .produce(new RuntimeReinitializedClassBuildItem("io.quarkus.vertx.runtime.jackson.QuarkusJacksonJsonCodec"));
        serviceProviderBuildItemBuildProducer
                .produce(ServiceProviderBuildItem.allProvidersFromClassPath(JsonFactory.class.getName()));
    }
}
