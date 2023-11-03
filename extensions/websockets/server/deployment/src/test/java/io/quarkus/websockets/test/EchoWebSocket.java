package io.quarkus.websockets.test;

import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoWebSocket {

    @Inject
    EchoService echoService;

    @OnMessage
    String echo(String msg) {
        return echoService.echo(msg);
    }

}
