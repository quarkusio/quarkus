package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

@WebSocket(path = "/echo-json")
public class EchoJson {

    @Inject
    EchoService echoService;

    @OnTextMessage
    Uni<JsonObject> echo(JsonObject msg) {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(new JsonObject().put("msg", echoService.echo(msg.getString("msg"))));
    }

}
