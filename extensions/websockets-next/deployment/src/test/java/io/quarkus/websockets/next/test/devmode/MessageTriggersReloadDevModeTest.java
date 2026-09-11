package io.quarkus.websockets.next.test.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientWebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * An application that only talks to its clients over open WebSocket connections, without any further HTTP request,
 * must still pick up source changes in dev mode: a message received on an open connection triggers the scan for
 * changes, and the restart closes the open connections so that clients reconnect to the updated application.
 */
public class MessageTriggersReloadDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root.addClass(Greeting.class));

    static Vertx vertx;
    static WebSocketClient client;

    @BeforeAll
    static void createClient() {
        vertx = Vertx.vertx();
        client = vertx.createWebSocketClient();
    }

    @AfterAll
    static void closeClient() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    public void testMessageOnOpenConnectionTriggersReload() {
        Connection connection = Connection.open();
        assertThat(connection.sendAndAwaitReply("Alice")).isEqualTo("Hello Alice");

        test.modifySourceFile(Greeting.class, s -> s.replace("\"Hello \"", "\"Hi \""));

        // no HTTP request is made: the message on the open connection is the only activity
        connection.send("Bob");
        await().atMost(Duration.ofSeconds(30)).until(connection::isClosed);

        connection = Connection.open();
        assertThat(connection.sendAndAwaitReply("Bob")).isEqualTo("Hi Bob");
    }

    static class Connection {

        final ClientWebSocket ws;
        final List<String> messages = new CopyOnWriteArrayList<>();
        volatile boolean closed;

        Connection(ClientWebSocket ws) {
            this.ws = ws;
        }

        static Connection open() {
            ClientWebSocket ws = client.webSocket();
            Connection connection = new Connection(ws);
            ws.textMessageHandler(connection.messages::add);
            ws.closeHandler(v -> connection.closed = true);
            ws.connect(new WebSocketConnectOptions().setHost("localhost").setPort(8080).setURI("/greeting"))
                    .toCompletionStage().toCompletableFuture().join();
            return connection;
        }

        void send(String message) {
            ws.writeTextMessage(message).toCompletionStage().toCompletableFuture().join();
        }

        String sendAndAwaitReply(String message) {
            int expected = messages.size() + 1;
            send(message);
            await().atMost(Duration.ofSeconds(10)).until(() -> messages.size() >= expected);
            return messages.get(messages.size() - 1);
        }

        boolean isClosed() {
            return closed;
        }
    }
}
