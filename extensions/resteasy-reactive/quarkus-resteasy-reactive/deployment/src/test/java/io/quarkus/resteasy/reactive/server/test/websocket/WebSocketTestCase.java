package io.quarkus.resteasy.reactive.server.test.websocket;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

public class WebSocketTestCase {

    @TestHTTPResource("/ws")
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebSocketResource.class));

    @Test
    public void testWebSocket() throws Exception {
        HttpClient httpClient = VertxCoreRecorder.getVertx().get().createHttpClient();
        try {
            final CompletableFuture<String> result = new CompletableFuture<>();
            httpClient.webSocket(uri.getPort(), uri.getHost(), uri.getPath(), new Handler<AsyncResult<WebSocket>>() {
                @Override
                public void handle(AsyncResult<WebSocket> event) {
                    if (event.failed()) {
                        result.completeExceptionally(event.cause());
                    } else {
                        event.result().exceptionHandler(new Handler<Throwable>() {
                            @Override
                            public void handle(Throwable event) {
                                result.completeExceptionally(event);
                            }
                        });
                        event.result().textMessageHandler(new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                result.complete(event);
                            }
                        });
                        event.result().writeTextMessage("Hello World");
                    }
                }
            });
            Assertions.assertEquals("Hello World", result.get());
        } finally {
            httpClient.close();
        }

    }

}
