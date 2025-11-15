package io.quarkus.it.resteasy.reactive.elytron;

import static io.restassured.RestAssured.port;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class StreamingOutputErrorHandlingTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int ITEMS_PER_BATCH = 100;
    private static final int BYTES_PER_CHUNK = "This is one chunk of data.\n"
            .getBytes(StandardCharsets.UTF_8).length;
    private static final long EXPECTED_BYTES_FIRST_BATCH = (long) ITEMS_PER_BATCH * BYTES_PER_CHUNK;
    private static final long EXPECTED_BYTES_COMPLETE = (long) ITEMS_PER_BATCH * 2 * BYTES_PER_CHUNK;

    private Vertx vertx;
    private HttpClient client;

    @BeforeEach
    public void setup() {
        vertx = Vertx.vertx();
        client = vertx.createHttpClient();
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (client != null) {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testStreamingOutputFailureMidStream() {
        AtomicLong byteCount = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-output-error/output?fail=true", latch,
                b -> byteCount.addAndGet(b.length()));

        Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
            ExecutionException ex = Assertions.assertThrows(ExecutionException.class,
                    latch::get,
                    "Client should have detected that the server reset the connection");

            Assertions.assertInstanceOf(HttpClosedException.class, ex.getCause(),
                    "Expected HttpClosedException when connection is reset mid-stream");
        });

        Assertions.assertEquals(EXPECTED_BYTES_FIRST_BATCH, byteCount.get(),
                "Should have received only the first batch of data before failure");
    }

    @Test
    public void testStreamingOutputSuccess() {
        AtomicLong byteCount = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-output-error/output?fail=false", latch,
                b -> byteCount.addAndGet(b.length()));

        Assertions.assertTimeoutPreemptively(TIMEOUT,
                () -> latch.get(),
                "StreamingOutput should complete successfully without errors");

        Assertions.assertEquals(EXPECTED_BYTES_COMPLETE, byteCount.get(),
                "Should have received all data when no errors occur");
    }

    private void sendRequest(String uri, CompletableFuture<Void> latch, Consumer<Buffer> bodyConsumer) {
        Handler<Throwable> failureHandler = latch::completeExceptionally;

        client.request(HttpMethod.GET, port, "localhost", uri)
                .onFailure(failureHandler)
                .onSuccess(request -> {
                    request.end();
                    request.connect()
                            .onFailure(failureHandler)
                            .onSuccess(response -> {
                                response.request().connection().closeHandler(v -> {
                                    failureHandler.handle(new HttpClosedException("Connection was closed"));
                                });

                                response.handler(buffer -> {
                                    if (buffer.length() > 0) {
                                        bodyConsumer.accept(buffer);
                                    }
                                });
                                response.exceptionHandler(failureHandler);
                                response.endHandler(v -> latch.complete(null));
                            });
                });
    }
}
