package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serdes;

import io.vertx.core.json.JsonArray;

public final class JsonArraySerde extends Serdes.WrapperSerde<JsonArray> {
    public JsonArraySerde() {
        super(new JsonArraySerializer(), new JsonArrayDeserializer());
    }
}
