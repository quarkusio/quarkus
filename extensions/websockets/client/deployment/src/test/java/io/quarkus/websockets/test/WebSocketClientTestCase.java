package io.quarkus.websockets.test;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import jakarta.websocket.ContainerProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

public class WebSocketClientTestCase {

    static Vertx vertx;
    static HttpServer server;

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestWebSocketClient.class);
                }
            });

    @BeforeAll
    public static void startVertxWebsocket() throws ExecutionException, InterruptedException {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        server.webSocketHandler(new Handler<ServerWebSocket>() {
            @Override
            public void handle(ServerWebSocket serverWebSocket) {
                serverWebSocket.writeTextMessage("Hello World").onComplete(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> voidAsyncResult) {
                        serverWebSocket.close();
                    }
                });
            }
        });
        server.listen(8081).toCompletionStage().toCompletableFuture().get();
    }

    @AfterAll
    public static void stop() throws TimeoutException {
        Future<Void> closeOperation;
        if (server != null) {
            closeOperation = server.close().andThen(ar -> vertx.close());
        } else {
            closeOperation = vertx.close();
        }
        closeOperation.await(20, TimeUnit.SECONDS);
    }

    @Test
    public void testWebsocketClient() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        ContainerProvider.getWebSocketContainer().connectToServer(client,
                new URI("ws", null, "localhost", 8081, "/", null, null));
        Assertions.assertEquals("Hello World", client.get());

    }
}
