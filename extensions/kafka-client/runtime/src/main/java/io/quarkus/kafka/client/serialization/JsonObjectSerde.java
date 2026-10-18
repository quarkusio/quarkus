package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serdes;

import io.vertx.core.json.JsonObject;

public final class JsonObjectSerde extends Serdes.WrapperSerde<JsonObject> {
    public JsonObjectSerde() {
        super(new JsonObjectSerializer(), new JsonObjectDeserializer());
    }
}
