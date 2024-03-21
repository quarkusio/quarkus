package io.quarkus.vertx.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.vertx.runtime.jackson.JsonArrayDeserializer;
import io.quarkus.vertx.runtime.jackson.JsonArraySerializer;
import io.quarkus.vertx.runtime.jackson.JsonObjectDeserializer;
import io.quarkus.vertx.runtime.jackson.JsonObjectSerializer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    @BuildStep
    JacksonModuleBuildItem registerJacksonSerDeser() {
        return new JacksonModuleBuildItem.Builder("VertxTypes")
                .add(JsonArraySerializer.class.getName(),
                        JsonArrayDeserializer.class.getName(),
                        JsonArray.class.getName())
                .add(JsonObjectSerializer.class.getName(),
                        JsonObjectDeserializer.class.getName(),
                        JsonObject.class.getName())
                .build();
    }
}
