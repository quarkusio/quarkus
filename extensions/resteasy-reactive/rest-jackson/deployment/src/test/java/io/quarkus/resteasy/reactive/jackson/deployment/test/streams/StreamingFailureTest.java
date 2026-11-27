package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;

/**
 * When a streamed {@code Multi} fails after items have been written, the status can no longer change, so the
 * response is reset: the client receives the items already sent, but not a well-formed body, and its read fails.
 * A failure before the first item goes through the exception mappers as usual.
 */
public class StreamingFailureTest {

    @TestHTTPResource
    URI uri;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FailingStreamResource.class, Message.class))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.SEVERE.intValue()
                    && "org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler".equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                    .isNotEmpty()
                    .allMatch(message -> message.contains("streaming")));

    @Test
    public void jsonArrayFailingBeforeTheFirstItemIsAnErrorResponse() throws Exception {
        Result result = get("/failing-streams/json/before-first-item");

        assertThat(result.status).isEqualTo(500);
        assertThat(result.body).doesNotStartWith("[");
        assertThat(result.readFailed).isFalse();
    }

    @Test
    public void jsonArrayFailingAfterItemsIsTruncated() throws Exception {
        Result result = get("/failing-streams/json/after-items");

        assertThat(result.status).isEqualTo(200);
        assertThat(result.readFailed).isTrue();
        assertThat(result.body).startsWith("[{\"name\":\"a\"}").doesNotContain("]");
    }

    @Test
    public void ndjsonFailingAfterItemsIsTruncated() throws Exception {
        Result result = get("/failing-streams/ndjson/after-items");

        assertThat(result.status).isEqualTo(200);
        assertThat(result.readFailed).isTrue();
        assertThat(result.body).startsWith("{\"name\":\"a\"}");
    }

    private Result get(String path) throws Exception {
        HttpClient client = vertx.createHttpClient();
        try {
            CompletableFuture<Integer> status = new CompletableFuture<>();
            CompletableFuture<Boolean> readFailed = new CompletableFuture<>();
            Buffer received = Buffer.buffer();
            client.request(HttpMethod.GET, uri.getPort(), uri.getHost(), path)
                    .compose(request -> request.send())
                    .onFailure(status::completeExceptionally)
                    .onSuccess(response -> {
                        status.complete(response.statusCode());
                        response.handler(received::appendBuffer);
                        response.exceptionHandler(ignored -> readFailed.complete(true));
                        response.endHandler(ignored -> readFailed.complete(false));
                    });
            int statusCode = status.get(10, TimeUnit.SECONDS);
            boolean failed = readFailed.get(10, TimeUnit.SECONDS);
            return new Result(statusCode, received.toString(), failed);
        } finally {
            client.close();
        }
    }

    private record Result(int status, String body, boolean readFailed) {
    }
}
