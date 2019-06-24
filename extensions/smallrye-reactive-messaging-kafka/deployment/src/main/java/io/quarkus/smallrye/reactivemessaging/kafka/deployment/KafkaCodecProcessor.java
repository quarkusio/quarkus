package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.vertx.kafka.client.serialization.BufferDeserializer;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.kafka.client.serialization.JsonArrayDeserializer;
import io.vertx.kafka.client.serialization.JsonArraySerializer;
import io.vertx.kafka.client.serialization.JsonObjectDeserializer;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;

public class KafkaCodecProcessor {

    static final Class<?>[] BUILT_INS = {
            JsonObjectSerializer.class,
            BufferSerializer.class,
            JsonArraySerializer.class,

            JsonObjectDeserializer.class,
            BufferDeserializer.class,
            JsonArrayDeserializer.class,
    };

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (Class<?> s : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, s.getName()));
        }
    }
}
