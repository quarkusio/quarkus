package io.quarkus.it.websocket;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoSocket {

    @OnMessage
    String echo(String msg) {
        return msg;
    }

}
