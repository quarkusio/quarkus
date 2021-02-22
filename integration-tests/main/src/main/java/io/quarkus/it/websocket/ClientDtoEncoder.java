package io.quarkus.it.websocket;

import java.io.IOException;
import java.io.Writer;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ClientDtoEncoder implements Encoder.TextStream<Dto> {
    @Override
    public void encode(Dto object, Writer writer) throws IOException {
        writer.append("[encoded]" + object.getContent());
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
