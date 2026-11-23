package io.quarkus.websockets.next.test.shutdown;

import static io.quarkus.websockets.next.test.shutdown.GracefulShutdownAssertions.assertShutdownDoesNotWait;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

/**
 * A WebSocket that was opened and closed again must not hold the graceful shutdown: its upgrade request is over and
 * nothing is in flight anymore.
 */
public class GracefulShutdownAfterWebSocketCloseTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(
                    root -> root.addClasses(ShutdownEndpoint.class, WSClient.class, GracefulShutdownAssertions.class))
            .overrideConfigKey("quarkus.shutdown.timeout", "3s");

    @Inject
    Vertx vertx;

    @Inject
    OpenConnections connections;

    @TestHTTPResource("shutdown")
    URI uri;

    @Test
    public void testClosedWebSocketDoesNotHoldGracefulShutdown() {
        try (WSClient client = WSClient.create(vertx).connect(uri)) {
            assertEquals("open", client.waitForNextMessage().toString());
            client.disconnect();
        }
        await().until(() -> connections.listAll().isEmpty());

        assertShutdownDoesNotWait();
    }

}
