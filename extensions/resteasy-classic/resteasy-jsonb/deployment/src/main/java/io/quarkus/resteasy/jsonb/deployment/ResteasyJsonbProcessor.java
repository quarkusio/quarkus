package io.quarkus.resteasy.jsonb.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.resteasy.jsonb.vertx.VertxJson;

public class ResteasyJsonbProcessor {

    private static final List<String> VERTX_SERIALIZERS = Arrays.asList(
            VertxJson.JsonObjectSerializer.class.getName(),
            VertxJson.JsonArraySerializer.class.getName());

    private static final List<String> VERTX_DESERIALIZERS = Arrays.asList(
            VertxJson.JsonObjectDeserializer.class.getName(),
            VertxJson.JsonArrayDeserializer.class.getName());

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_JSONB));
    }

    @BuildStep
    public void registerVertxJsonSupport(
            BuildProducer<JsonbSerializerBuildItem> serializers,
            BuildProducer<JsonbDeserializerBuildItem> deserializers) {
        serializers.produce(new JsonbSerializerBuildItem(VERTX_SERIALIZERS));
        deserializers.produce(new JsonbDeserializerBuildItem(VERTX_DESERIALIZERS));
    }
}
