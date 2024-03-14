package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@WebSocket(path = "/echo-json-array")
public class EchoJsonArray {

    @Inject
    EchoService echoService;

    @OnTextMessage
    Uni<JsonArray> echo(JsonArray msg) {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(
                new JsonArray().add(
                        new JsonObject().put("msg", echoService.echo(msg.getJsonObject(0).getString("msg")))));
    }

}
