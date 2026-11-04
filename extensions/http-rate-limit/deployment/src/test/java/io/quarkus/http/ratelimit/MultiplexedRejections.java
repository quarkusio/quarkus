package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.StreamResetException;
import io.vertx.ext.web.Router;

/**
 * Checks shared by the HTTP/2 and HTTP/3 tests: one connection carries many requests, so a refused request whose body
 * exceeds {@code quarkus.http.limits.max-body-size} must end its own stream, not the connection, or every other
 * request on that connection fails with it. Expects a rule on {@code /api/*} of one permit per IP, a
 * {@code max-body-size} of 1K, and {@link SlowRoute}.
 */
final class MultiplexedRejections {

    private MultiplexedRejections() {
    }

    static void rejectionIsAnswered(HttpClientAgent client, int port, HttpVersion version) throws TimeoutException {
        spendPermit(client, port);
        Response response = whole("the refused request", client.request(HttpMethod.GET, port, "localhost", "/api/x")
                .compose(HttpClientRequest::send));
        assertThat(response.version()).isEqualTo(version);
        assertThat(response.status()).isEqualTo(429);
        assertThat(response.retryAfter()).isNotBlank();
        assertThat(response.body()).isEqualTo("Too Many Requests");
    }

    /**
     * A refused request that declares a body over the limit.
     */
    static void declaredBodyEndsOnlyItsOwnStream(HttpClientAgent client, int port) throws TimeoutException {
        assertOnlyTheRefusedStreamEnds(client, port, false);
    }

    /**
     * A refused request whose body grows over the limit.
     */
    static void streamedBodyEndsOnlyItsOwnStream(HttpClientAgent client, int port) throws TimeoutException {
        assertOnlyTheRefusedStreamEnds(client, port, true);
    }

    private static void assertOnlyTheRefusedStreamEnds(HttpClientAgent client, int port, boolean streamed)
            throws TimeoutException {
        spendPermit(client, port);
        // in flight on the connection while the refused request is answered
        Future<Response> slow = read("the other request on the connection",
                client.request(HttpMethod.GET, port, "localhost", "/slow")
                        .compose(HttpClientRequest::send));
        HttpClientRequest refused = client.request(HttpMethod.POST, port, "localhost", "/api/x")
                .await(10, TimeUnit.SECONDS);
        Future<Response> refusedResponse = read("the refused request", refused.response());
        Promise<Throwable> cutOff = Promise.promise();
        refused.exceptionHandler(cutOff::tryComplete);
        if (streamed) {
            refused.setChunked(true);
            // 32K, far over the 1K limit; writes after the stream ended fail, which does not matter here
            for (int i = 0; i < 64; i++) {
                refused.write(Buffer.buffer("x".repeat(512)));
            }
        } else {
            refused.putHeader(HttpHeaders.CONTENT_LENGTH, "1000000000");
            refused.writeHead();
        }

        Response rejection = refusedResponse.await(10, TimeUnit.SECONDS);
        assertThat(rejection.status()).isEqualTo(429);
        // the refused request may not send the rest of its body
        if (rejection.version() == HttpVersion.HTTP_2) {
            // its stream is reset
            assertThat(cutOff.future().await(10, TimeUnit.SECONDS)).isInstanceOf(StreamResetException.class);
        } else {
            // the server stops reading the stream; the Vert.x HTTP/3 client does not report it, but its writes stall,
            // which without the cut-off complete at once
            assertThatThrownBy(() -> refused.write(Buffer.buffer("x".repeat(512))).await(1, TimeUnit.SECONDS))
                    .isInstanceOf(TimeoutException.class);
        }
        Response other = slow.await(10, TimeUnit.SECONDS);
        assertThat(other.status()).isEqualTo(200);
        assertThat(other.body()).isEqualTo("slow");
        assertThat(other.connection()).isSameAs(rejection.connection());

        // and the connection keeps serving requests
        Response next = whole("the next request on the connection", client.request(HttpMethod.GET, port, "localhost", "/slow")
                .compose(HttpClientRequest::send));
        assertThat(next.status()).isEqualTo(200);
        assertThat(next.connection()).isSameAs(rejection.connection());
    }

    /**
     * Reads the body in the same chain as the head, on the event loop: read later, it may already be gone.
     */
    private static Future<Response> read(String request, Future<HttpClientResponse> response) {
        return response.compose(r -> r.body().map(body -> new Response(r.version(), r.statusCode(),
                r.getHeader("Retry-After"), body.toString(), r.request().connection())))
                .recover(failure -> Future.failedFuture(new AssertionError(request + " failed", failure)));
    }

    private static Response whole(String request, Future<HttpClientResponse> response) throws TimeoutException {
        return read(request, response).await(10, TimeUnit.SECONDS);
    }

    record Response(HttpVersion version, int status, String retryAfter, String body, HttpConnection connection) {
    }

    /**
     * Spends the one permit of this client's address, if an earlier test has not already.
     */
    private static void spendPermit(HttpClientAgent client, int port) throws TimeoutException {
        client.request(HttpMethod.POST, port, "localhost", "/api/x")
                .compose(HttpClientRequest::send)
                .compose(HttpClientResponse::body)
                .await(10, TimeUnit.SECONDS);
    }

    /**
     * Answers {@code GET /slow} after a second, outside every rule, so that it is still in flight when a request on the
     * same connection is refused.
     */
    @ApplicationScoped
    static class SlowRoute {

        void register(@Observes Router router) {
            router.get("/slow").handler(rc -> rc.vertx().setTimer(1000, t -> rc.response().end("slow")));
        }
    }
}
