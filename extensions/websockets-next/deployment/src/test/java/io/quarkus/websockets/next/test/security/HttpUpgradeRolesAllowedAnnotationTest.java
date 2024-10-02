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

public class HttpUpgradeRolesAllowedAnnotationTest extends SecurityTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            AdminEndpoint.class));

    @TestHTTPResource("admin-end")
    URI adminEndpointUri;

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

    @RolesAllowed("admin")
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

    @RolesAllowed({ "admin", "user" })
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
