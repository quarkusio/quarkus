package io.quarkus.websockets.next.test.security;

import static io.quarkus.websockets.next.test.security.SecurityTestBase.basicAuth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class AuthenticationExpiredTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Endpoint.class, TestIdentityProvider.class,
                    TestIdentityController.class, WSClient.class, ExpiredIdentityAugmentor.class, SecurityTestBase.class));

    @Test
    public void testConnectionClosedWhenAuthExpires() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), endUri);

            long threeSecondsFromNow = Duration.ofMillis(System.currentTimeMillis()).plusSeconds(3).toMillis();
            for (int i = 1; true; i++) {
                if (client.isClosed()) {
                    break;
                } else if (System.currentTimeMillis() > threeSecondsFromNow) {
                    Assertions.fail("Authentication expired, therefore connection should had been closed");
                }
                try {
                    client.sendAndAwaitReply("Hello #" + i + " from ");
                } catch (RuntimeException e) {
                    // this sometimes fails as connection is closed when waiting for the reply
                    break;
                }
            }

            var receivedMessages = client.getMessages().stream().map(Buffer::toString).toList();
            assertTrue(receivedMessages.size() > 2, receivedMessages.toString());
            assertTrue(receivedMessages.contains("Hello #1 from admin"), receivedMessages.toString());
            assertTrue(receivedMessages.contains("Hello #2 from admin"), receivedMessages.toString());
            assertEquals(1008, client.closeStatusCode(), "Expected close status 1008, but got " + client.closeStatusCode());

            Awaitility
                    .await()
                    .atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertTrue(Endpoint.CLOSED_MESSAGE.get()
                            .startsWith("Connection closed with reason 'Authentication expired'")));

            assertTrue(client.isClosed());
        }
    }

    @Singleton
    public static class ExpiredIdentityAugmentor implements SecurityIdentityAugmentor {

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni
                    .createFrom()
                    .item(QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addAttribute("quarkus.identity.expire-time", expireIn2Seconds())
                            .build());
        }

        private static long expireIn2Seconds() {
            return Duration.ofMillis(System.currentTimeMillis())
                    .plusSeconds(2)
                    .toSeconds();
        }
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        static final AtomicReference<String> CLOSED_MESSAGE = new AtomicReference<>();

        @Inject
        SecurityIdentity currentIdentity;

        @Authenticated
        @OnTextMessage
        String echo(String message) {
            return message + currentIdentity.getPrincipal().getName();
        }

        @OnClose
        void close(CloseReason reason, WebSocketConnection connection) {
            CLOSED_MESSAGE.set("Connection closed with reason '%s': %s".formatted(reason.getMessage(), connection));
        }
    }

}
