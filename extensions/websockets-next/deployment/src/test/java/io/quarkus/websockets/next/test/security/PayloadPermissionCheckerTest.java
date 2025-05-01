package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

public class PayloadPermissionCheckerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            AdminEndpoint.class, InclusiveEndpoint.class, MetaAnnotationEndpoint.class,
                            StringEndpointReadPermissionMetaAnnotation.class, ProductEndpoint.class))
            .overrideConfigKey("quarkus.websockets-next.server.unhandled-failure-strategy", "close");

    @Inject
    Vertx vertx;

    @TestHTTPResource("admin-end")
    URI adminEndpointUri;

    @TestHTTPResource("meta-annotation")
    URI metaAnnotationEndpointUri;

    @TestHTTPResource("inclusive-end")
    URI inclusiveEndpointUri;

    @TestHTTPResource("product")
    URI productUri;

    @BeforeEach
    public void prepareUsers() {
        TestIdentityController.resetRoles().add("admin", "admin", "admin").add("almighty", "almighty").add("user", "user");
    }

    @Test
    public void testHandledFailure() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), productUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("1");
            // shouldn't close as user declared @OnError
            client.waitForMessages(2);
            // can't see product 1
            assertEquals("forbidden:user,endpointId:io.quarkus.websockets.next.test.security.ProductEndpoint",
                    client.getMessages().get(1).toString());
            // can see product 2
            client.sendAndAwait("2");
            client.waitForMessages(3);
            String response = client.getMessages().get(2).toString();
            assertTrue(response != null && response.contains("Product 2"));
        }

        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), productUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            // admin can see product 1, unlike the user
            client.sendAndAwait("1");
            client.waitForMessages(2);
            String response = client.getMessages().get(1).toString();
            assertTrue(response != null && response.contains("Product 1"));
        }
    }

    @Test
    public void testInsufficientRights() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), adminEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("true");
            Awaitility.await().until(client::isClosed);
            assertEquals(1008, client.closeStatusCode());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), adminEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("false");
            Awaitility.await().until(client::isClosed);
            assertEquals(1008, client.closeStatusCode());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), adminEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("true");
            client.waitForMessages(2);
            assertEquals("true", client.getMessages().get(1).toString());
        }
    }

    @Test
    public void testMetaAnnotation() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), metaAnnotationEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("true");
            Awaitility.await().until(client::isClosed);
            assertEquals(1008, client.closeStatusCode());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), metaAnnotationEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("false");
            Awaitility.await().until(client::isClosed);
            assertEquals(1008, client.closeStatusCode());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), metaAnnotationEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("true");
            client.waitForMessages(2);
            assertEquals("true", client.getMessages().get(1).toString());
        }
    }

    @Test
    public void testInclusivePermissions() {
        Stream.of("admin", "user").forEach(name -> {
            try (WSClient client = new WSClient(vertx)) {
                client.connect(basicAuth(name, name), inclusiveEndpointUri);
                client.waitForMessages(1);
                assertEquals("ready", client.getMessages().get(0).toString());
                client.sendAndAwait("hello");
                Awaitility.await().until(client::isClosed);
                assertEquals(1008, client.closeStatusCode());
            }
        });
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("almighty", "almighty"), inclusiveEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }
    }

    static WebSocketConnectOptions basicAuth(String username, String password) {
        return new WebSocketConnectOptions().addHeader(HttpHeaders.AUTHORIZATION.toString(),
                new UsernamePasswordCredentials(username, password).applyHttpChallenge(null).toHttpAuthorization());
    }

    @WebSocket(path = "/inclusive-end")
    public static class InclusiveEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @PermissionsAllowed(value = { "perm1", "perm2" }, inclusive = true)
        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @WebSocket(path = "/admin-end")
    public static class AdminEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @PermissionsAllowed("endpoint:read")
        @OnTextMessage
        String echo(boolean canRead) {
            return "" + canRead;
        }

    }

    @WebSocket(path = "/meta-annotation")
    public static class MetaAnnotationEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @StringEndpointReadPermissionMetaAnnotation
        @OnTextMessage
        String echo(boolean canRead) {
            return "" + canRead;
        }

    }

    @ApplicationScoped
    public static class Checker {

        @PermissionChecker("endpoint:read")
        boolean canDoReadOnEndpoint(SecurityIdentity securityIdentity, boolean canRead) {
            return securityIdentity.getPrincipal().getName().equals("admin") && canRead;
        }

        @PermissionChecker("perm1")
        boolean hasPerm1(SecurityIdentity securityIdentity) {
            String principalName = securityIdentity.getPrincipal().getName();
            return principalName.equals("admin") || principalName.equals("almighty");
        }

        @PermissionChecker("perm2")
        Uni<Boolean> hasPerm2(SecurityIdentity securityIdentity) {
            String principalName = securityIdentity.getPrincipal().getName();
            return Uni.createFrom().item(Boolean.valueOf(principalName.equals("user") || principalName.equals("almighty")));
        }

    }
}
