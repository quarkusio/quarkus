package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
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

public class HttpUpgradeAuthenticatedAnnotationTest extends SecurityTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            PublicEndpoint.class, PublicEndpoint.SubEndpoint.class));

    @TestHTTPResource("public-end")
    URI publicEndUri;

    @TestHTTPResource("public-end/sub")
    URI subEndUri;

    @Test
    public void testSubEndpoint() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(publicEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }

        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client.connect(subEndUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), subEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("sub-endpoint", client.getMessages().get(1).toString());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), subEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("sub-endpoint:forbidden:user", client.getMessages().get(1).toString());
        }
    }

    @WebSocket(path = "/public-end")
    public static class PublicEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @Authenticated
        @WebSocket(path = "/sub")
        public static class SubEndpoint {

            @Inject
            CurrentIdentityAssociation currentIdentity;

            @OnOpen
            String open() {
                return "ready";
            }

            @RolesAllowed("admin")
            @OnTextMessage
            String echo(String message) {
                return "sub-endpoint";
            }

            @OnError
            String error(ForbiddenException t) {
                return "sub-endpoint:forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
            }
        }

    }

    @Authenticated
    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @RolesAllowed("admin")
        @OnTextMessage
        String echo(String message) {
            if (!currentIdentity.getIdentity().hasRole("admin")) {
                throw new IllegalStateException();
            }
            return message;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }
}
