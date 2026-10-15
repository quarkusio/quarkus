package io.quarkus.it.oidc.dev.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
class SignalsSecurityTest {

    private static final OidcTestClient oidcTestClient = new OidcTestClient();

    @TestHTTPResource("/chat")
    URI chatUri;

    @AfterAll
    static void close() {
        oidcTestClient.close();
    }

    @Test
    void sendSignalFromRestEndpoint() {
        RestAssured.given()
                .delete("/signals/clear")
                .then()
                .statusCode(204);
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/signals/roles-allowed/admin")
                .then()
                .statusCode(200)
                .body(Matchers.is("roles-allowed-admin:bob [user]"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/signals/roles-allowed/user")
                .then()
                .statusCode(200)
                .body(Matchers.is("roles-allowed-user:bob [user]"));
        // only 'user' arrived, not 'admin'
        RestAssured.given()
                .get("/signals/messages")
                .then()
                .statusCode(200)
                .body(Matchers.is("roles-allowed-user:bob [user]"));
    }

    @Test
    void sendSignalFromWebSocketEndpoint() throws InterruptedException {
        RestAssured.given().delete("/signals/clear").then().statusCode(204);

        var vertx = Vertx.vertx();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(chatUri.getHost());
        options.setPort(chatUri.getPort());
        options.setURI(chatUri.getPath() + "/IF");
        options.addHeader("Authorization", "Bearer " + oidcTestClient.getAccessToken("alice", "alice"));
        try {
            client
                    .connect(options)
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                messages.add(msg);
                                messagesLatch.countDown();
                            });
                            // We will use this socket to write a message later on
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
            ws1.get().writeTextMessage("hello");
            assertTrue(messagesLatch.await(5, TimeUnit.SECONDS), "Messages: " + messages);
            assertEquals(2, messages.size(), "Messages: " + messages);
            assertEquals("opened", messages.get(0));
            assertEquals("hello alice", messages.get(1));

            Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> RestAssured.given()
                    .get("/signals/messages")
                    .then()
                    .statusCode(200)
                    .body(Matchers.containsString("websockets:hello alice")));
        } finally {
            client.close().await();
            vertx.close().await();
        }
    }

    private static String getAccessToken(String user) {
        return oidcTestClient.getAccessToken(user, user);
    }
}
