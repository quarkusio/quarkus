package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpMethod;

public class InputStreamResponseErrorTestCase {

    private static final long LARGE_SIZE = 2L * 1024 * 1024 * 1024;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, GeneratedInputStream.class));

    @TestHTTPResource
    URL url;

    private Vertx vertx;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        client = vertx.createHttpClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void streamLargerThanTheHeapIsFullyWritten() throws Exception {
        AtomicLong received = new AtomicLong();
        download("/input-stream?size=" + LARGE_SIZE, received).get(2, TimeUnit.MINUTES);
        assertThat(received.get()).isEqualTo(LARGE_SIZE);
    }

    @Test
    void failureAfterTheHeadersAbortsTheResponse() {
        AtomicLong received = new AtomicLong();
        CompletableFuture<Void> result = download("/input-stream?size=" + LARGE_SIZE + "&failAt=" + LARGE_SIZE / 2,
                received);

        assertThatThrownBy(() -> result.get(2, TimeUnit.MINUTES))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HttpClosedException.class);
        assertThat(received.get()).isLessThan(LARGE_SIZE);
    }

    @Test
    void failureBeforeTheHeadersReturnsServerError() {
        get("/input-stream?size=1024&failAt=10")
                .then()
                .statusCode(500);
    }

    private CompletableFuture<Void> download(String uri, AtomicLong received) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        client.request(HttpMethod.GET, url.getPort(), url.getHost(), uri)
                .onFailure(result::completeExceptionally)
                .onSuccess(request -> request.connect()
                        .onFailure(result::completeExceptionally)
                        .onSuccess(response -> {
                            response.request().connection().closeHandler(
                                    v -> result.completeExceptionally(new HttpClosedException("Connection was closed")));
                            response.exceptionHandler(result::completeExceptionally);
                            response.handler(buffer -> received.addAndGet(buffer.length()));
                            response.endHandler(v -> result.complete(null));
                        }));
        return result;
    }

    @Path("input-stream")
    public static class Resource {

        @GET
        public Response get(@QueryParam("size") long size, @QueryParam("failAt") @DefaultValue("-1") long failAt) {
            return Response.ok(new GeneratedInputStream(size, failAt), MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
        }
    }

    public static class GeneratedInputStream extends InputStream {

        private final long size;
        private final long failAt;
        private long position;

        public GeneratedInputStream(long size, long failAt) {
            this.size = size;
            this.failAt = failAt;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b, 0, 1) == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (position == size) {
                return -1;
            }
            if (position == failAt) {
                throw new IOException("Failure while reading the entity");
            }
            long limit = failAt >= 0 ? Math.min(size, failAt) : size;
            int n = (int) Math.min(len, limit - position);
            Arrays.fill(b, off, off + n, (byte) 'a');
            position += n;
            return n;
        }
    }
}
