package io.quarkus.it.websocket;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/recoder", encoders = ServerDtoEncoder.class, decoders = ServerDtoDecoder.class)
public class RecodingSocket {
    @OnMessage
    public Dto recode(Dto input) {
        Dto result = new Dto();
        result.setContent("[recoded]" + input.getContent());
        return result;
    }
}
