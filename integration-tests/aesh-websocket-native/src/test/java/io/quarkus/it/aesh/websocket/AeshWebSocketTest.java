package io.quarkus.it.aesh.websocket;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
public class AeshWebSocketTest {

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Test
    public void testWebSocketConnectionAndCommand() throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            client.connect(new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath()))
                    .onComplete(ar -> {
                        if (ar.failed()) {
                            latch.countDown();
                            return;
                        }
                        var ws = ar.result();

                        ws.textMessageHandler(msg -> {
                            messages.add(msg);
                            if (msg.contains("Hello Native!")) {
                                latch.countDown();
                            }
                        });

                        ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");

                        vertx.setTimer(500, id -> {
                            ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello --name=Native\\r\"}");
                        });
                    });

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            String allOutput = String.join("", messages);

            Assertions.assertThat(completed)
                    .as("Expected to receive 'Hello Native!' in WebSocket output within 15s. Received: %s", allOutput)
                    .isTrue();
            Assertions.assertThat(allOutput).contains("Hello Native!");
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            vertx.close();
        }
    }

    @Test
    public void testCdiInjectionOverWebSocket() throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            client.connect(new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath()))
                    .onComplete(ar -> {
                        if (ar.failed()) {
                            latch.countDown();
                            return;
                        }
                        var ws = ar.result();

                        ws.textMessageHandler(msg -> {
                            messages.add(msg);
                            if (msg.contains("Hello CDI from service!")) {
                                latch.countDown();
                            }
                        });

                        ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");

                        vertx.setTimer(500, id -> {
                            ws.writeTextMessage("{\"action\":\"read\",\"data\":\"cdi-greet --name=CDI\\r\"}");
                        });
                    });

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            String allOutput = String.join("", messages);

            Assertions.assertThat(completed)
                    .as("Expected to receive 'Hello CDI from service!' in WebSocket output within 15s. Received: %s",
                            allOutput)
                    .isTrue();
            Assertions.assertThat(allOutput).contains("Hello CDI from service!");
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            vertx.close();
        }
    }
}
