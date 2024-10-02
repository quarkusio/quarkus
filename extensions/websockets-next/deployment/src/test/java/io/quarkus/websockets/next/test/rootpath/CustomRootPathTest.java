package io.quarkus.websockets.next.test.rootpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class CustomRootPathTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            }).overrideConfigKey("quarkus.http.root-path", "/api");

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testEndpoint() {
        assertTrue(testUri.toString().contains("/api"));
        try (WSClient client = WSClient.create(vertx).connect(testUri)) {
            assertEquals("monty", client.sendAndAwaitReply("monty").toString());
        }
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        String process(String message) throws InterruptedException {
            return message;
        }

    }

}
