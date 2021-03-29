package io.quarkus.resteasy.reactive.server.test.websocket;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;

@Path("ws")
public class WebSocketResource {

    @GET
    public void echoWebSocket(ServerWebSocket serverWebSocket) {
        serverWebSocket.textMessageHandler(new Handler<String>() {
            @Override
            public void handle(String event) {
                serverWebSocket.writeTextMessage(event);
            }
        });
    }
}
