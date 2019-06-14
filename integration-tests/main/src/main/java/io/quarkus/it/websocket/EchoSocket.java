package io.quarkus.it.websocket;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoSocket {

    @OnMessage
    String echo(String msg) {
        return msg;
    }

}
