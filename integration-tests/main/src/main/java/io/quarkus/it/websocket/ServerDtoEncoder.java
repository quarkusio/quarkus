package io.quarkus.it.websocket;

import java.io.Writer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

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
