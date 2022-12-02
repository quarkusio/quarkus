package io.quarkus.vertx.graphql.it;

import static io.restassured.RestAssured.given;
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
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;

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
                .addSubProtocol("graphql-ws").setURI("/graphql");
        JsonObject init = new JsonObject().put("type", ApolloWSMessageType.CONNECTION_INIT.getText());
        String graphql = "{\"id\" : \"2\", \"type\" : \"start\", \"payload\" : { \"query\" : \"{ hello }\" } }";
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
            AtomicReference<String> lastReceivedType = new AtomicReference<>();
            if (ws.succeeded()) {
                WebSocket webSocket = ws.result();
                webSocket.handler(message -> {
                    if (lastReceivedType.compareAndSet(null, ApolloWSMessageType.CONNECTION_ACK.getText())) {
                        // Go ack, wait for the next message (ka)
                    } else if (lastReceivedType.compareAndSet("connection_ack",
                            ApolloWSMessageType.CONNECTION_KEEP_ALIVE.getText())) {
                        webSocket.write(Buffer.buffer(graphql));
                    } else {
                        JsonObject json = message.toJsonObject();
                        String type = json.getString("type");
                        if (ApolloWSMessageType.DATA.getText().equals(type)) {
                            wsFuture.complete(message.toJsonObject());
                        } else {
                            wsFuture.completeExceptionally(new RuntimeException(
                                    format("Unexpected message type: %s\nMessage: %s", type, message.toString())));
                        }
                    }
                });
                webSocket.write(init.toBuffer());
            } else {
                wsFuture.completeExceptionally(ws.cause());
            }
        });

        JsonObject json = wsFuture.get(1, TimeUnit.MINUTES);
        assertNotNull(json);
        assertEquals("2", json.getString("id"));
        assertEquals("data", json.getString("type"));
        assertEquals("world", json.getJsonObject("payload").getJsonObject("data").getString("hello"));
    }

}
