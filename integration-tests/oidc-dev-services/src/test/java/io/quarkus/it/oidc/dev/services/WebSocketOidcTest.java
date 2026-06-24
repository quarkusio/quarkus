package io.quarkus.it.oidc.dev.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.quarkus.it.oidc.dev.services.SecurityIdentityUpdateWebSocket.ResponseDto;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
public class WebSocketOidcTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @TestHTTPResource("/chat")
    URI chatUri;

    @TestHTTPResource("/security-identity-update")
    URI identityUpdateUri;

    @TestHTTPResource("/expired-updated-identity")
    URI expiredUpdatedIdentityUri;

    @TestHTTPResource("/change-in-updated-identity-roles")
    URI updatedIdentityRoleUri;

    @Inject
    Vertx vertx;

    @Inject
    ObjectMapper objectMapper;

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
        options.setHost(chatUri.getHost());
        options.setPort(chatUri.getPort());
        options.setURI(chatUri.getPath() + "/IF");
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

    @Test
    public void testSecurityIdentityUpdate() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        List<ResponseDto> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(identityUpdateUri.getHost());
        options.setPort(identityUpdateUri.getPort());
        options.setURI(identityUpdateUri.getPath());
        options.setSubProtocols(List.of("quarkus"));
        options.addHeader("Authorization", "Bearer " + oidcTestClient.getAccessToken("alice", "alice"));
        try {
            client
                    .connect(options)
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                try {
                                    var responseDto = objectMapper.readValue(msg,
                                            SecurityIdentityUpdateWebSocket.ResponseDto.class);
                                    messages.add(responseDto);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));

            // first authorized call the endpoint without any identity update
            ws1.get().writeTextMessage(createRequest("hello", null));
            Awaitility.await().atMost(TIMEOUT)
                    .untilAsserted(() -> assertEquals(1, messages.size(), "Messages: " + messages));
            var responseDto = messages.get(0);
            assertEquals("hello", responseDto.message());
            assertEquals("alice", responseDto.injectedIdentityBefore());

            // now update identity
            Thread.sleep(1000); // makes sure that tokens have different expiration time
            ws1.get().writeTextMessage(createRequest("hello", oidcTestClient.getAccessToken("alice", "alice")));
            Awaitility.await().atMost(TIMEOUT)
                    .untilAsserted(() -> assertEquals(2, messages.size(), "Messages: " + messages));
            responseDto = messages.get(1);
            assertEquals("hello", responseDto.message());
            assertEquals("alice", responseDto.injectedIdentityBefore());
            assertEquals("alice", responseDto.updatedIdentityPrincipal());
            assertFalse(responseDto.identitiesIdentical());

            // and update identity again
            Thread.sleep(1000); // makes sure that tokens have different expiration time
            ws1.get().writeTextMessage(createRequest("hello", oidcTestClient.getAccessToken("alice", "alice")));
            Awaitility.await().atMost(TIMEOUT)
                    .untilAsserted(() -> assertEquals(3, messages.size(), "Messages: " + messages));
            responseDto = messages.get(2);
            assertEquals("hello", responseDto.message());
            assertEquals("alice", responseDto.injectedIdentityBefore());
            assertEquals("alice", responseDto.updatedIdentityPrincipal());
            assertFalse(responseDto.identitiesIdentical());

            // permission checker only allows 'hello', so expect connection will be closed
            // this checks that permission checker is actually invoked, which is important because it checks for us
            // that updated identity is passed to the checker on the next invocation
            Assertions.assertNull(ws1.get().closeStatusCode());
            ws1.get().writeTextMessage(createRequest("bye", oidcTestClient.getAccessToken("alice", "alice")));
            Awaitility.await().atMost(TIMEOUT).untilAsserted(
                    () -> {
                        Assertions.assertNotNull(ws1.get().closeStatusCode());
                        assertEquals(WebSocketCloseStatus.POLICY_VIOLATION.code(), (int) ws1.get().closeStatusCode());
                    });
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testUpdatedSecurityIdentityExpiration() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(1);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(expiredUpdatedIdentityUri.getHost());
        options.setPort(expiredUpdatedIdentityUri.getPort());
        options.setURI(expiredUpdatedIdentityUri.getPath());
        options.setSubProtocols(List.of("quarkus"));
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
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));

            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
            // create new token, identity augmentor will make sure it expires in 2 seconds
            var nextToken = oidcTestClient.getAccessToken("alice", "alice");
            ws1.get().writeTextMessage(nextToken);
            assertTrue(messagesLatch.await(5, TimeUnit.SECONDS), "Messages: " + messages);
            assertEquals(1, messages.size(), "Messages: " + messages);
            assertEquals("bye", messages.get(0));

            // after about 2 seconds, updated SecurityIdentity will expire, so expect connection closed
            Assertions.assertNull(ws1.get().closeStatusCode());
            Awaitility.await().atMost(TIMEOUT).untilAsserted(
                    () -> {
                        Assertions.assertNotNull(ws1.get().closeStatusCode());
                        assertEquals(WebSocketCloseStatus.POLICY_VIOLATION.code(), (int) ws1.get().closeStatusCode());
                    });
            var closeReason = ws1.get().closeReason();
            Assertions.assertTrue(closeReason.contains("Authentication expired"), closeReason);
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testUpdatedSecurityIdentityHasDifferentRole()
            throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(1);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(updatedIdentityRoleUri.getHost());
        options.setPort(updatedIdentityRoleUri.getPort());
        options.setURI(updatedIdentityRoleUri.getPath());
        options.setSubProtocols(List.of("quarkus"));
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
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));

            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
            // create new token, identity augmentor will drop 'admin' role and use 'user' role instead
            var nextToken = oidcTestClient.getAccessToken("alice", "alice");
            ws1.get().writeTextMessage(nextToken);
            assertTrue(messagesLatch.await(5, TimeUnit.SECONDS), "Messages: " + messages);
            assertEquals(1, messages.size(), "Messages: " + messages);
            assertEquals("cheers", messages.get(0));

            // once the identity is updated, we expect that '@RolesAllowed' on the WebSocket endpoint class
            // is reapplied every single time, so it must recognize that 'admin' role is gone and forbid access
            nextToken = oidcTestClient.getAccessToken("alice", "alice");
            ws1.get().writeTextMessage(nextToken);

            // by default the authorization failure should result in closing of the connection
            Assertions.assertNull(ws1.get().closeStatusCode());
            Awaitility.await().atMost(TIMEOUT).untilAsserted(
                    () -> {
                        Assertions.assertNotNull(ws1.get().closeStatusCode());
                        assertEquals(WebSocketCloseStatus.POLICY_VIOLATION.code(), (int) ws1.get().closeStatusCode());
                    });
            // assert the second message never got in, in other words, the access to the endpoint was forbidden
            assertEquals(1, messages.size(), "Messages: " + messages);
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    private String createRequest(String message, String accessToken) {
        SecurityIdentityUpdateWebSocket.Metadata metadata;
        if (accessToken != null) {
            metadata = new SecurityIdentityUpdateWebSocket.Metadata(accessToken);
        } else {
            metadata = null;
        }
        var requestDto = new SecurityIdentityUpdateWebSocket.RequestDto(message, metadata);
        try {
            return objectMapper.writeValueAsString(requestDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
