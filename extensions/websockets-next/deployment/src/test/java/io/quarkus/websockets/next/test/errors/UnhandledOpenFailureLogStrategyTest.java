package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class UnhandledOpenFailureLogStrategyTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(EchoOpenError.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.server.unhandled-failure-strategy", "log");

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testErrorDoesNotCloseConnection() throws InterruptedException {
        try (WSClient client = WSClient.create(vertx).connect(testUri)) {
            assertTrue(EchoOpenError.OPEN_CALLED.await(5, TimeUnit.SECONDS));
            client.sendAndAwait("foo");
            client.waitForMessages(1);
            assertEquals("foo", client.getLastMessage().toString());
        }
    }

}
