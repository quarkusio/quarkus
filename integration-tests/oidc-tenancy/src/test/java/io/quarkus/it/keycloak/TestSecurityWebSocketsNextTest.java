package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.AnnotationBasedTenantTest.getTokenWithRole;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.Tenant;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
public class TestSecurityWebSocketsNextTest {

    @TestHTTPResource("/ws/echo/authenticated-http-upgrade")
    URI authenticatedHttpUpgradeUri;

    @TestHTTPResource("/ws/echo/payload-authorization")
    URI payloadAuthorizationUri;

    @TestHTTPResource("/ws/echo/security-identity-injection")
    URI securityIdentityInjectionUri;

    @TestHTTPResource("/ws/echo/authorized-security-identity-injection")
    URI authorizedSecurityIdentityInjectionUri;

    @Inject
    Vertx vertx;

    @Test
    @TestSecurity(user = "Martin")
    public void testHttpUpgradeAuthenticated() throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(false, authenticatedHttpUpgradeUri);
    }

    @Test
    public void testHttpUpgradeAuthenticated_failure() {
        RuntimeException actualFailure = assertThrows(RuntimeException.class,
                () -> callWebSocketEndpoint(true, authenticatedHttpUpgradeUri));
        assertInstanceOf(UpgradeRejectedException.class, actualFailure.getCause());
    }

    @Test
    @TestSecurity(user = "Martin")
    public void testPayloadAuthorization() throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(false, payloadAuthorizationUri);
    }

    @Test
    @TestSecurity(user = "Martin")
    public void testPayloadAuthorization_failure() throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(false, payloadAuthorizationUri, "access denied");
    }

    @Test
    @TestSecurity(user = "Martin")
    public void testSecurityIdentityInjection() throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(null, false, securityIdentityInjectionUri, "Martin", "Hey");
    }

    @Test
    @TestSecurity(user = "Martin", authMechanism = "Bearer")
    public void testInjectionPoint_bearerTokenMechanism() throws ExecutionException, InterruptedException, TimeoutException {
        String aliceToken = getTokenWithRole("admin");
        callWebSocketEndpoint(aliceToken, false, securityIdentityInjectionUri, "alice", "Hey");
    }

    @Test
    @TestSecurity(user = "Martin", authMechanism = "Bearer")
    public void testSecuredInjectionPoint_bearerTokenMechanism()
            throws ExecutionException, InterruptedException, TimeoutException {
        // the endpoint is unreachable with 'Martin', because he does not have the 'admin' role
        // hence we know that the token overrides the @TestSecurity
        String aliceToken = getTokenWithRole("admin");
        callWebSocketEndpoint(aliceToken, false, authorizedSecurityIdentityInjectionUri, "alice", "Hey");
    }

    private void callWebSocketEndpoint(boolean expectFailure, URI endpointURI, String message)
            throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(null, expectFailure, endpointURI, null, message);
    }

    private void callWebSocketEndpoint(boolean expectFailure, URI endpointURI)
            throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(expectFailure, endpointURI, "hello");
    }

    private void callWebSocketEndpoint(String token, boolean expectFailure, URI endpointURI, String expectedPrincipal,
            String expectedMessage) throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<io.vertx.core.http.WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(endpointURI.getHost());
        options.setPort(endpointURI.getPort());
        options.setURI(endpointURI.getPath());
        if (token != null) {
            options.addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token);
        }
        CountDownLatch throwableLatch = new CountDownLatch(1);
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        try {
            var connection = client.connect(options);
            connection
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            io.vertx.core.http.WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                messages.add(msg);
                                messagesLatch.countDown();
                            });
                            // We will use this socket to write a message later on
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throwable.set(r.cause());
                            throwableLatch.countDown();
                        }
                    });
            if (expectFailure) {
                throwableLatch.await(5, TimeUnit.SECONDS);
                var cause = throwable.get();
                if (cause != null) {
                    throw new RuntimeException(throwable.get());
                } else {
                    Assertions.fail("Expected HTTP upgrade failure");
                }
            } else {
                Assertions.assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
                ws1.get().writeTextMessage(expectedMessage);
                Assertions.assertTrue(messagesLatch.await(5, TimeUnit.SECONDS), "Messages: " + messages);
                Assertions.assertEquals(2, messages.size(), "Messages: " + messages);
                Assertions.assertEquals("ready", messages.get(0));
                if (expectedPrincipal != null) {
                    Assertions.assertEquals("message: " + expectedMessage + " " + expectedPrincipal, messages.get(1));
                } else {
                    Assertions.assertEquals("message: " + expectedMessage, messages.get(1));
                }
                ws1.get().close();
            }
        } finally {
            if (ws1.get() != null && !ws1.get().isClosed()) {
                ws1.get().close();
            }
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Authenticated
    @WebSocket(path = "/ws/echo/authenticated-http-upgrade")
    public static class AuthenticatedHttpUpgradeEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return "message: " + message;
        }

    }

    @WebSocket(path = "/ws/echo/payload-authorization")
    public static class PayloadAuthorizationEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @PermissionsAllowed("canReadMessage")
        @OnTextMessage
        String echo(String message) {
            return "message: " + message;
        }

        @PermissionChecker("canReadMessage")
        boolean canReadMessage(String message) {
            return "hello".equals(message);
        }

        @OnError
        String onError(ForbiddenException forbiddenException) {
            return "message: access denied";
        }
    }

    @Tenant("tenant-public-key")
    @WebSocket(path = "/ws/echo/security-identity-injection")
    public static class SecurityIdentityInjectionEndpoint {

        @Inject
        SecurityIdentity securityIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return "message: " + message + " " + securityIdentity.getPrincipal().getName();
        }

    }

    @RolesAllowed("admin")
    @Tenant("tenant-public-key")
    @WebSocket(path = "/ws/echo/authorized-security-identity-injection")
    public static class AuthorizedSecurityIdentityInjectionEndpoint {

        @Inject
        SecurityIdentity securityIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return "message: " + message + " " + securityIdentity.getPrincipal().getName();
        }

    }
}
