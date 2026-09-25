package io.quarkus.websockets.test.handshake;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/handshake", configurator = HandshakeRecordingConfigurator.class)
public class HandshakeWebSocket {

    @OnMessage
    String echo(String msg) {
        return msg;
    }
}
