package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.UpgradeRejectedException;

public class ClientUpgradeFailureTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class, AlwaysFailing.class);
            });

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    public void testClient() throws InterruptedException {
        UpgradeRejectedException e = assertThrows(UpgradeRejectedException.class,
                () -> connector.baseUri(uri).connectAndAwait());
        assertEquals(500, e.getStatus());
        assertTrue(AlwaysFailing.REJECTED.get());
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        @OnOpen
        String open() {
            return "Hello!";
        }
    }

    @Singleton
    public static class AlwaysFailing implements HttpUpgradeCheck {

        static final AtomicBoolean REJECTED = new AtomicBoolean();

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            REJECTED.set(true);
            return CheckResult.rejectUpgrade(500);
        }

    }

    @WebSocketClient(path = "/end")
    public static class ClientEndpoint {

        @OnOpen
        String open() {
            return "Hello!";
        }

    }

}
