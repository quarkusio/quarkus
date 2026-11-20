package io.quarkus.resteasy.reactive.server.test.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;

/**
 * Verifies that a client disconnect releases the worker thread that is writing
 * an infinite blocking {@link InputStream} response, instead of letting it spin
 * forever. Covers both HTTP/1.1 and HTTP/2 (h2c).
 */
public class StreamDisconnectTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(InfiniteStreamResource.class));

    @Inject
    Vertx vertx;

    @BeforeEach
    void resetLatches() {
        InfiniteStreamResource.reset();
    }

    @Test
    public void testWorkerReleasedOnHttp1Disconnect() throws Exception {
        assertWorkerReleasedOnDisconnect(HttpVersion.HTTP_1_1);
    }

    @Test
    public void testWorkerReleasedOnHttp2Disconnect() throws Exception {
        assertWorkerReleasedOnDisconnect(HttpVersion.HTTP_2);
    }

    private void assertWorkerReleasedOnDisconnect(HttpVersion version) throws Exception {
        HttpClientOptions opts = new HttpClientOptions().setProtocolVersion(version);
        if (version == HttpVersion.HTTP_2) {
            opts.setHttp2ClearTextUpgrade(false);
        }
        HttpClient client = vertx.createHttpClient(opts);
        try {
            Promise<Void> gotData = Promise.promise();
            client.request(HttpMethod.GET, RestAssured.port, "localhost", "/infinite-stream")
                    .compose(HttpClientRequest::send)
                    .compose(resp -> {
                        resp.handler(buf -> gotData.tryComplete());
                        return gotData.future();
                    })
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            Assertions.assertTrue(
                    InfiniteStreamResource.streamClosed.await(10, TimeUnit.SECONDS),
                    "Worker thread was not released within 10 s after client disconnect (" + version + ")");
        } finally {
            try {
                client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    @Path("/infinite-stream")
    public static class InfiniteStreamResource {

        static volatile CountDownLatch streamClosed;
        static final AtomicBoolean started = new AtomicBoolean();

        static void reset() {
            streamClosed = new CountDownLatch(1);
            started.set(false);
        }

        @GET
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream infiniteStream() {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    started.set(true);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                    return 'x';
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    started.set(true);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                    int n = Math.min(len, 1024);
                    Arrays.fill(b, off, off + n, (byte) 'x');
                    return n;
                }

                @Override
                public void close() throws IOException {
                    streamClosed.countDown();
                    super.close();
                }
            };
        }
    }
}
