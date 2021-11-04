package io.quarkus.reactivemessaging.websocket.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.utils.VertxFriendlyLock;
import io.quarkus.reactivemessaging.websocket.WebSocketClient;
import io.quarkus.reactivemessaging.websocket.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

class WebSocketSourceTest {

    private static Vertx vertx;
    private static WebSocketClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Consumer.class, WebSocketClient.class, VertxFriendlyLock.class))
            .withConfigurationResource("websocket-source-test-application.properties");

    @TestHTTPResource("my-ws")
    URI wsSourceUri;

    @TestHTTPResource("my-ws-json")
    URI wsSourceForJsonUri;

    @TestHTTPResource("my-ws-buffer-13")
    URI wsSourceBuffer13Uri;

    @Inject
    Consumer consumer;

    @Test
    void shouldPassTextContentAndHeaders() {
        client.connect(wsSourceUri).send("test-message");

        await("wait for message to be consumed")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> consumer.getMessages(), hasSize(1));
        String payload = consumer.getMessages().get(0);
        assertThat(payload).isEqualTo("test-message");
    }

    @Test
    void shouldConsumeJson() {
        client.connect(wsSourceForJsonUri).send("{\"field\": \"json\"}");

        await("wait for json message to be consumed and deserialized")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> consumer.getDtos(), hasSize(1));
        assertThat(consumer.getDtos().get(0).getField()).isEqualTo("json");
    }

    @Test
    void shouldBuffer13IfConfigured() {
        shouldBuffer(13, wsSourceBuffer13Uri);
    }

    @Test
    void shouldBuffer8ByDefault() {
        shouldBuffer(8, wsSourceUri);
    }

    void shouldBuffer(int bufferSize, URI wsUri) {
        WebSocketClient.WsConnection connection = client.connect(wsUri);
        int messagesToSend = 17;
        // 1 message should start being consumed, `messagesToSend` should be buffered, the rest should respond with 503

        consumer.pause();
        ExecutorService executorService = Executors.newFixedThreadPool(messagesToSend);

        for (int i = 0; i < messagesToSend; i++) {
            executorService.submit(() -> connection.send("some-text"));
        }

        Integer expectedFailureCount = messagesToSend - bufferSize - 1;

        await("assert " + expectedFailureCount + " failures")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> connection.getResponses().stream().filter("BUFFER_OVERFLOW"::equals).count(),
                        equalTo(expectedFailureCount.longValue()));

        consumer.resume();

        await("all processing finished")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> connection.getResponses().size(), equalTo(messagesToSend));

        assertThat(consumer.getMessages()).hasSize(messagesToSend - expectedFailureCount);
    }

    @AfterEach
    void cleanUp() {
        consumer.clear();
    }

    @BeforeAll
    static void setUp() {
        vertx = Vertx.vertx();
        client = new WebSocketClient(vertx);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        vertx.close(ignored -> shutdownLatch.countDown());
        shutdownLatch.await(10, TimeUnit.SECONDS);
    }
}
