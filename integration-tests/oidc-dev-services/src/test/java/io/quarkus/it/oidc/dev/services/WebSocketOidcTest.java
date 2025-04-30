package io.quarkus.it.oidc.dev.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
public class WebSocketOidcTest {

    @TestHTTPResource("/chat")
    URI uri;

    @Inject
    Vertx vertx;

    private static final OidcTestClient oidcTestClient = new OidcTestClient();

    @AfterAll
    public static void close() {
        oidcTestClient.close();
    }

    @Test
    public void testDocumentedTokenPropagationUsingSubProtocol()
            throws InterruptedException, ExecutionException, TimeoutException {
        // verify that handler documented in WebSockets Next reference
        // propagates "Sec-WebSocket-Protocol" as Authorization header
        // and authentication is successful
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(uri.getHost());
        options.setPort(uri.getPort());
        options.setURI(uri.getPath() + "/IF");
        options.setSubProtocols(
                List.of("quarkus",
                        "quarkus-http-upgrade#Authorization#Bearer " + oidcTestClient.getAccessToken("alice", "alice")));
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
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

}
