package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;

/**
 * Keeps a Dev UI JSON-RPC websocket open across a hot reload, like a browser tab does, and checks that the connection
 * keeps working without Vert.x reporting uncaught exceptions (see issue #57277).
 */
public class JsonRpcWebSocketReloadTest {

    private static final int PORT = Integer.getInteger("quarkus.http.test-port", 8080);

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClass(ReloadMarkerResource.class))
            .setLogRecordPredicate(r -> true);

    @Test
    public void testSocketSurvivesHotReload() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            WebSocketClient client = vertx.createWebSocketClient(
                    new WebSocketClientOptions().setDefaultHost("localhost").setDefaultPort(PORT));
            WebSocket socket = client.connect("/q/dev-ui/json-rpc-ws").await(30, TimeUnit.SECONDS);
            try {
                assertThat(request(socket, 1)).satisfies(m -> assertThat(hasId(m, 1)).isTrue()).doesNotContain("\"error\"");

                config.clearLogRecords();
                config.modifySourceFile(ReloadMarkerResource.class, s -> s.replace("\"before\"", "\"after\""));
                RestAssured.get("/reload-marker").then().statusCode(200);

                // The connection is either still usable, or cleanly closed so that the client can reconnect.
                if (!socket.isClosed()) {
                    String response = request(socket, 2);
                    assertThat(response == null || hasId(response, 2)).isTrue();
                }
            } finally {
                client.close().await(30, TimeUnit.SECONDS);
            }
            assertThat(config.getLogRecords())
                    .noneMatch(r -> r.getMessage() != null && r.getMessage().contains("Uncaught exception received by Vert.x"))
                    .noneMatch(r -> r.getThrown() instanceof NullPointerException);
        } finally {
            vertx.close().await(30, TimeUnit.SECONDS);
        }
    }

    private static boolean hasId(String message, int id) {
        return Pattern.compile("\"id\"\\s*:\\s*" + id + "\\b").matcher(message).find();
    }

    /** Returns the response text, or null if the socket got closed before answering. */
    private static String request(WebSocket socket, int id) throws Exception {
        CompletableFuture<String> response = new CompletableFuture<>();
        // The server also pushes unsolicited messages (e.g. log lines) on this socket, so only look at our response.
        socket.textMessageHandler(m -> {
            if (hasId(m, id)) {
                response.complete(m);
            }
        });
        socket.closeHandler(v -> response.complete(null));
        socket.writeTextMessage("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"devui-logstream_history\",\"params\":{}}");
        return response.get(30, TimeUnit.SECONDS);
    }
}
