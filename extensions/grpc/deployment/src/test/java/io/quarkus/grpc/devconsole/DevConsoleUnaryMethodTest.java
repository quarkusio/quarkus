package io.quarkus.grpc.devconsole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

public class DevConsoleUnaryMethodTest {

    private static final Logger log = Logger.getLogger(DevConsoleUnaryMethodTest.class);

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addPackage(MutinyGreeterGrpc.class.getPackage())
                    .addClass(MutinyHelloService.class));

    @Test
    public void websocketTest() throws Exception {
        Vertx vertx = Vertx.vertx();

        try {
            List<String> incomingMessages = new CopyOnWriteArrayList<>();
            HttpClient client = vertx.createHttpClient();

            client.webSocket(8080, "localhost", "/q/dev/io.quarkus.quarkus-grpc/grpc-test", result -> {
                if (result.failed()) {
                    log.error("failure making a web socket connection", result.cause());
                    return;
                }
                WebSocket webSocket = result.result();
                webSocket.handler(buffer -> incomingMessages.add(buffer.toString()));
                webSocket
                        .writeTextMessage("{\"id\": 123, \"serviceName\": \"helloworld.Greeter\",\"methodName\": \"SayHello\"" +
                                ", \"content\":  \"{\\\"name\\\": \\\"Martin\\\"}\"}");
            });

            await().atMost(5, TimeUnit.SECONDS)
                    .until(() -> incomingMessages.size() > 0);

            assertThat(incomingMessages).hasSize(2);

            Optional<String> payloadMessage = incomingMessages.stream().filter(msg -> msg.contains("PAYLOAD")).findFirst();
            assertThat(payloadMessage).isNotEmpty();
            assertThat(payloadMessage.get()).contains("Hello Martin");

        } finally {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close(whatever -> latch.countDown());
            if (!latch.await(30, TimeUnit.SECONDS)) {
                log.warn("Waiting for the test vertx instance to stop failed");
            }
        }
    }

}
