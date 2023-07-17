package io.quarkus.vertx.graphql.it;

import static io.restassured.RestAssured.given;
import static io.vertx.ext.web.handler.graphql.ws.MessageType.COMPLETE;
import static io.vertx.ext.web.handler.graphql.ws.MessageType.CONNECTION_ACK;
import static io.vertx.ext.web.handler.graphql.ws.MessageType.CONNECTION_INIT;
import static io.vertx.ext.web.handler.graphql.ws.MessageType.NEXT;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ws.MessageType;

@QuarkusTest
class VertxGraphqlTest {
    private static Vertx vertx;

    public static int getPortFromConfig() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081);
    }

    @BeforeAll
    public static void initializeVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void closeVertx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close((h) -> latch.countDown());
        latch.await();
    }

    @Test
    public void testGraphQlQuery() {
        given().contentType(ContentType.JSON).body("{ \"query\" : \"{ hello }\" }")
                .when().post("/graphql")
                .then().log().ifValidationFails().statusCode(200).body("data.hello", is("world"));
    }

    @Test
    public void testWebSocketSubProtocol() throws Exception {
        HttpClient httpClient = vertx.createHttpClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions().setPort(getPortFromConfig())
                .addSubProtocol("graphql-transport-ws").setURI("/graphql");
        JsonObject init = new JsonObject().put("type", CONNECTION_INIT.getText());
        String graphql = "{\"id\" : \"2\", \"type\" : \"subscribe\", \"payload\" : { \"query\" : \"{ hello }\" } }";
        CompletableFuture<JsonObject> wsFuture = new CompletableFuture<>();
        wsFuture.whenComplete((r, t) -> httpClient.close());

        /*
         * Protocol:
         * --> connection_init -->
         * <-- connection_ack <--
         * <-- ka (keep-alive) <--
         * -----> start ------>
         * <----- data <-------
         */

        httpClient.webSocket(options, ws -> {
            AtomicReference<MessageType> lastReceivedType = new AtomicReference<>();
            AtomicReference<JsonObject> result = new AtomicReference<>();
            if (ws.succeeded()) {
                WebSocket webSocket = ws.result();
                webSocket.handler(message -> {
                    JsonObject json = message.toJsonObject();
                    MessageType messageType = MessageType.from(json.getString("type"));
                    if (messageType == CONNECTION_ACK) {
                        if (lastReceivedType.compareAndSet(null, CONNECTION_ACK)) {
                            webSocket.write(Buffer.buffer(graphql));
                            return;
                        }
                    } else if (messageType == NEXT) {
                        if (lastReceivedType.compareAndSet(CONNECTION_ACK, NEXT)) {
                            result.set(json);
                            return;
                        }
                    } else if (messageType == COMPLETE) {
                        if (lastReceivedType.compareAndSet(NEXT, COMPLETE)) {
                            wsFuture.complete(result.get());
                            return;
                        }
                    }
                    wsFuture.completeExceptionally(new RuntimeException(
                            format("Unexpected message type: %s\nMessage: %s", messageType.getText(), message)));
                });
                webSocket.write(init.toBuffer());
            } else {
                wsFuture.completeExceptionally(ws.cause());
            }
        });

        JsonObject json = wsFuture.get(1, TimeUnit.MINUTES);
        assertNotNull(json);
        assertEquals("2", json.getString("id"));
        assertEquals(NEXT.getText(), json.getString("type"));
        assertEquals("world", json.getJsonObject("payload").getJsonObject("data").getString("hello"));
    }

}
