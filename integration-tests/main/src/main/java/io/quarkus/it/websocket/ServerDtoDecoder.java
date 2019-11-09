package io.quarkus.it.websocket;

import java.io.Reader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class ServerDtoDecoder implements Decoder.TextStream<Dto> {
    @Override
    public Dto decode(Reader reader) {
        try (JsonReader jsonReader = Json.createReader(reader)) {
            JsonObject jsonObject = jsonReader.readObject();
            Dto result = new Dto();
            result.setContent(jsonObject.getString("content"));
            return result;
        }
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
