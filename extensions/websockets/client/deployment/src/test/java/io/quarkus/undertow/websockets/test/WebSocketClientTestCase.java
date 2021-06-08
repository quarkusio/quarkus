package io.quarkus.undertow.websockets.test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.websocket.ContainerProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

public class WebSocketClientTestCase {

    static Vertx vertx;
    static HttpServer server;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public static void stop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        if (server != null) {
            server.close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> voidAsyncResult) {
                    vertx.close(new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> event) {
                            latch.countDown();
                        }
                    });
                }
            });
        } else if (vertx != null) {
            vertx.close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    latch.countDown();
                }
            });
        }
        latch.await(20, TimeUnit.SECONDS);
    }

    @Test
    public void testWebsocketClient() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        ContainerProvider.getWebSocketContainer().connectToServer(client,
                new URI("ws", null, "localhost", 8081, "/", null, null));
        Assertions.assertEquals("Hello World", client.get());

    }
}
