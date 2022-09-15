package io.quarkus.it.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class ClientDtoDecoder implements Decoder.TextStream<Dto> {
    @Override
    public Dto decode(Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String input = bufferedReader.readLine(); // expecting one line input
            Dto result = new Dto();
            result.setContent("[decoded]" + input);
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
