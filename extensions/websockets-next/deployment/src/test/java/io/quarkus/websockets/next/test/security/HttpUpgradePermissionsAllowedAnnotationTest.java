package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.http.UpgradeRejectedException;

public class HttpUpgradePermissionsAllowedAnnotationTest extends SecurityTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            AdminEndpoint.class, InclusiveEndpoint.class, MetaAnnotationEndpoint.class,
                            StringEndpointReadPermissionMetaAnnotation.class));

    @TestHTTPResource("admin-end")
    URI adminEndpointUri;

    @TestHTTPResource("meta-annotation")
    URI metaAnnotationEndpointUri;

    @TestHTTPResource("inclusive-end")
    URI inclusiveEndpointUri;

    @Test
    public void testInsufficientRights() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(basicAuth("user", "user"), adminEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"));
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), adminEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }
    }

    @Test
    public void testMetaAnnotation() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(basicAuth("user", "user"), metaAnnotationEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"));
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), metaAnnotationEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }
    }

    @Test
    public void testInclusivePermissions() {
        Stream.of("admin", "user").forEach(name -> {
            try (WSClient client = new WSClient(vertx)) {
                CompletionException ce = assertThrows(CompletionException.class,
                        () -> client.connect(basicAuth(name, name), inclusiveEndpointUri));
                Throwable root = ExceptionUtil.getRootCause(ce);
                assertInstanceOf(UpgradeRejectedException.class, root);
                assertTrue(root.getMessage().contains("403"));
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

    @PermissionsAllowed(value = { "perm1", "perm2" }, inclusive = true)
    @WebSocket(path = "/inclusive-end")
    public static class InclusiveEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @PermissionsAllowed("endpoint:read")
    @WebSocket(path = "/admin-end")
    public static class AdminEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @StringEndpointReadPermissionMetaAnnotation
    @WebSocket(path = "/meta-annotation")
    public static class MetaAnnotationEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @PermissionsAllowed(value = { "endpoint:connect", "endpoint:read" })
    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @PermissionsAllowed("endpoint:read")
        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }
}
