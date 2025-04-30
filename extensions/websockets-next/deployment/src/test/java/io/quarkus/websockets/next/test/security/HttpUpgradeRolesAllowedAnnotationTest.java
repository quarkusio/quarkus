package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.spi.runtime.SecurityEvent;
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
                    .addAsResource(new StringAsset("""
                            quarkus.security.events.enabled=false
                            """), "application.properties")
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            AdminEndpoint.class, SecurityEventObserver.class));

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

        // assert no security events when the events are disabled
        assertEquals(0, SecurityEventObserver.count.get());
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

    public static class SecurityEventObserver {

        private static final AtomicInteger count = new AtomicInteger();

        void observe(@Observes SecurityEvent securityEvent) {
            count.incrementAndGet();
        }
    }
}
