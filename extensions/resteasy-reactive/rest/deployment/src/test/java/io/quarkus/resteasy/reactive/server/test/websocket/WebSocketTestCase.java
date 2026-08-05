package io.quarkus.resteasy.reactive.server.test.websocket;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.http.WebSocketClient;

public class WebSocketTestCase {

    @TestHTTPResource("/ws")
    URI uri;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebSocketResource.class));

    @Test
    public void testWebSocket() throws Exception {
        WebSocketClient wsClient = VertxCoreRecorder.getVertx().get().createWebSocketClient();
        try {
            final CompletableFuture<String> result = new CompletableFuture<>();
            wsClient.connect(uri.getPort(), uri.getHost(), uri.getPath()).onComplete(event -> {
                if (event.failed()) {
                    result.completeExceptionally(event.cause());
                } else {
                    event.result().exceptionHandler(result::completeExceptionally);
                    event.result().textMessageHandler(result::complete);
                    event.result().writeTextMessage("Hello World");
                }
            });
            Assertions.assertEquals("Hello World", result.get());
        } finally {
            wsClient.close();
        }

    }

}
