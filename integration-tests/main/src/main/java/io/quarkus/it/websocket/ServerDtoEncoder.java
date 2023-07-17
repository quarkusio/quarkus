package io.quarkus.it.websocket;

import java.io.Writer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class ServerDtoEncoder implements Encoder.TextStream<Dto> {
    @Override
    public void encode(Dto object, Writer writer) {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("content", object.getContent())
                .build();
        try (JsonWriter jsonWriter = Json.createWriter(writer)) {
            jsonWriter.writeObject(jsonObject);
        }
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
