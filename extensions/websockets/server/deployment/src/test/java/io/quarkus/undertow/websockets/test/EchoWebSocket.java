package io.quarkus.undertow.websockets.test;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoWebSocket {

    @Inject
    EchoService echoService;

    @OnMessage
    String echo(String msg) {
        return echoService.echo(msg);
    }

}
