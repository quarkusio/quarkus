package io.quarkus.undertow.websockets.test;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoWebSocket {

    @OnMessage
    String echo(String msg) {
        return msg;
    }

}
