package io.quarkus.websockets.next.test.subsocket;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket("/sub")
public class Sub {

    @OnMessage
    Uni<String> echo(String msg) {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(msg);
    }

    @WebSocket("/sub/{id}")
    public static class SubSub {

        @Inject
        WebSocketServerConnection connection;

        @OnMessage
        Uni<String> echo(String msg) {
            assertTrue(Context.isOnEventLoopThread());
            return Uni.createFrom().item(connection.pathParam("id") + ":" + msg);
        }

        @WebSocket("/sub/{name}")
        public static class SubSubSub {

            @Inject
            WebSocketServerConnection connection;

            @OnMessage
            Uni<String> echo(String msg) {
                assertTrue(Context.isOnEventLoopThread());
                return Uni.createFrom().item(connection.pathParam("id") + ":" + connection.pathParam("name") + ":" + msg);
            }

        }

    }
}
