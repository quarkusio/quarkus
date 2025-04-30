package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class UnhandledOpenFailureDefaultStrategyTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(EchoOpenError.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() throws InterruptedException {
        try (WSClient client = WSClient.create(vertx).connect(testUri)) {
            assertTrue(EchoOpenError.OPEN_CALLED.await(5, TimeUnit.SECONDS));
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> client.isClosed());
            assertEquals(WebSocketCloseStatus.INTERNAL_SERVER_ERROR.code(), client.closeStatusCode());
        }
    }

}
