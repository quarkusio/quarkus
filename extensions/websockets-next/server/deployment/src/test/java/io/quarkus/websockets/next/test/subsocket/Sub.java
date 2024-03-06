package io.quarkus.websockets.next.test.subsocket;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket(path = "/sub")
public class Sub {

    @OnMessage
    Uni<String> echo(String msg) {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(msg);
    }

    @WebSocket(path = "/sub/{id}")
    public static class SubSub {

        @Inject
        WebSocketConnection connection;

        @OnMessage
        Uni<String> echo(String msg) {
            assertTrue(Context.isOnEventLoopThread());
            return Uni.createFrom().item(connection.pathParam("id") + ":" + msg);
        }

        @WebSocket(path = "/sub/{name}")
        public static class SubSubSub {

            @Inject
            WebSocketConnection connection;

            @OnMessage
            Uni<String> echo(String msg) {
                assertTrue(Context.isOnEventLoopThread());
                return Uni.createFrom().item(connection.pathParam("id") + ":" + connection.pathParam("name") + ":" + msg);
            }

        }

    }
}
