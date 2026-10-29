package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.export.GrpcResponse;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * The exporter timeout, {@code quarkus.otel.exporter.otlp.timeout} (10 s by default), bounds each attempt of an export.
 * <p>
 * The senders used to apply it as the read idle timeout of the connection instead. That timer ticks every
 * {@code timeout} after the last read on the connection, whatever request it belonged to, which causes two problems:
 * <ul>
 * <li>an export in flight at a tick has its connection closed after the collector received it, and the retry sends the
 * batch twice, see #57107</li>
 * <li>any data read on the connection restarts the timer, so a response that keeps trickling in but never completes
 * never times out, and the export hangs forever, see #57904</li>
 * </ul>
 * The tests use a {@link #TIMEOUT} of 1 s so they run quickly. The scenarios are the real ones, scaled down.
 */
public class SenderTimeoutTest {

    // The exporter timeout of the senders under test, quarkus.otel.exporter.otlp.timeout scaled down from its 10 s default
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class));

    @Inject
    Vertx vertx;

    private HttpServer collector;
    private VertxGrpcSender grpcSender;
    private VertxHttpSender httpSender;

    @AfterEach
    void cleanup() throws Exception {
        if (grpcSender != null) {
            grpcSender.shutdown();
        }
        if (httpSender != null) {
            httpSender.shutdown();
        }
        if (collector != null) {
            collector.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    /**
     * An export that is in flight when the old idle timer ticks must still be sent once.
     * <p>
     * Only one timeout is involved, {@link #TIMEOUT}. The other two values are not timeouts:
     * <ul>
     * <li>export cadence: export #1 at 0 s, export #2 at 0.9 s, just before the old idle tick at 1 s</li>
     * <li>collector latency: the collector answers request 2 after 300 ms, so it is still in flight at the tick</li>
     * </ul>
     * This is the real scenario of #57107 scaled down by 10: a 10 s timeout and one span every 20 s, so an export
     * starts just before the tick at 20 s after the last read.
     *
     * <pre>
     * Real app on main, timeout 10 s, one span every 20 s:
     * t(s)  0         10              ~19.99  20
     * req 1 [=]
     * idle            tick (no export in flight, ignored)
     * req 2                           [=======X        closed by the tick at 20
     * req 3                                   [====]   retry = duplicate
     *
     * This test on main, timeout 1 s, exports 0.9 s apart:
     * t(s)  0.0      0.9   1.0        1.3
     * req 1 [==]
     * req 2          [=====X                 closed by the tick at 1.0, response 2 never arrives
     * req 3                [=========]       retry = duplicate
     * 3 requests: fails
     *
     * This test with the fix, no idle timer, 1 s limit per attempt:
     * t(s)  0.0      0.9        1.2       1.9
     * req 1 [==]
     * req 2          [==========]            response 2 at 1.2, inside its limit (0.9 to 1.9)
     * 2 requests: passes
     * </pre>
     */
    @Test
    public void grpcExportStartingOnAnIdleConnectionIsSentOnce() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> {
            long delay = requests.incrementAndGet() == 1 ? 1 : 300; // collector answer delay: 1 ms for req 1, then 300 ms
            request.endHandler(v -> vertx.setTimer(delay, id -> grpcOk(request.response())));
        });
        grpcSender = grpcSender(); // timeout set here.

        exportGrpc();
        Thread.sleep(900); // export cadence: export #2 starts just before the old idle tick at TIMEOUT
        GrpcResponse response = exportGrpc();

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertThat(requests).hasValue(2);
    }

    /**
     * Same scenario as {@link #grpcExportStartingOnAnIdleConnectionIsSentOnce()}, over OTLP/HTTP.
     *
     * <pre>
     * On main:
     * t(s)  0.0      0.9   1.0        1.3
     * req 1 [==]
     * req 2          [=====X                 closed by the tick at 1.0
     * req 3                [=========]       retry = duplicate
     * 3 requests: fails
     *
     * With the fix:
     * t(s)  0.0      0.9        1.2
     * req 1 [==]
     * req 2          [==========]            response 2 OK
     * 2 requests: passes
     * </pre>
     */
    @Test
    public void httpExportStartingOnAnIdleConnectionIsSentOnce() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> {
            long delay = requests.incrementAndGet() == 1 ? 1 : 300;
            request.endHandler(v -> vertx.setTimer(delay, id -> request.response().setStatusCode(200).end()));
        });
        httpSender = httpSender();

        exportHttp();
        Thread.sleep(900);
        HttpResponse response = exportHttp();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(requests).hasValue(2);
    }

    /**
     * An export whose response never completes must time out and be retried, even though data keeps arriving.
     * <p>
     * For request 1 the collector sends the response headers and the start of a message announced with 1000 bytes,
     * then one byte every 100 ms. Request 2 is answered at once. Each byte is a read on the connection, so on main it
     * restarts the idle timer and the export never ends, see #57904. With the fix, the 1 s limit of the attempt does
     * not care about reads.
     *
     * <pre>
     * On main:
     * t(s)  0.0                         10.0
     * req 1 [=.=.=.=.=.=.=.=.=.=.=.=.=.=.=  ...   a byte every 100 ms, each one restarts the idle timer
     * the export never ends: the test gives up after 10 s and fails
     *
     * With the fix:
     * t(s)  0.0         1.0  ~1.1
     * req 1 [=.=.=.=.=.=X                  cancelled at its 1 s limit
     * req 2                  [=]           retry, answered at once
     * 2 requests and still 2 after 1.5 s more: passes
     * </pre>
     *
     * The second check makes sure the cancelled attempt does not start another retry once the export has completed.
     */
    @Test
    public void grpcExportStalledOnABusyConnectionIsRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> {
            if (requests.incrementAndGet() == 1) {
                HttpServerResponse response = request.response();
                response.putHeader("content-type", "application/grpc");
                // no compression flag, 1000 bytes of message that never all arrive
                response.write(Buffer.buffer(new byte[] { 0, 0, 0, 0x03, (byte) 0xE8 }));
                // one more byte of the message per keepBusy tick (100 ms): data keeps arriving, the message never completes
                keepBusy(response, id -> response.write(Buffer.buffer(new byte[1])));
            } else {
                grpcOk(request.response());
            }
        });
        grpcSender = grpcSender();

        GrpcResponse response = exportGrpc();

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertNoMoreRequests(requests, 2);
    }

    /**
     * Same as {@link #grpcExportStalledOnABusyConnectionIsRetried()}, with the traffic reported in #57904: the collector
     * never answers request 1, not even with the response headers, but keeps the HTTP/2 connection alive with a PING
     * every 100 ms, like a load balancer, a sidecar proxy or a NAT keep-alive would. A PING is a read on the connection
     * too, so on main it restarts the idle timer and the export never ends. The limit of the attempt does not care
     * about the traffic on the connection, so the attempt is cancelled at 1 s and retried.
     *
     * <pre>
     * With the fix:
     * t(s)  0.0         1.0  ~1.1
     * req 1 [ping.ping.ping.X               no response at all, cancelled at its 1 s limit
     * req 2                  [=]           retry, answered at once
     * 2 requests and still 2 after 1.5 s more: passes
     * </pre>
     */
    @Test
    public void grpcExportStalledOnAPingedConnectionIsRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> {
            if (requests.incrementAndGet() == 1) {
                // nothing is ever written on the stream, only PINGs on the connection that carries it
                keepBusy(request.response(), id -> request.connection().ping(Buffer.buffer(new byte[8])));
            } else {
                grpcOk(request.response());
            }
        });
        grpcSender = grpcSender();

        GrpcResponse response = exportGrpc();

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertNoMoreRequests(requests, 2);
    }

    /**
     * Same scenario as {@link #grpcExportStalledOnABusyConnectionIsRetried()}, over OTLP/HTTP: for request 1 the
     * collector sends a 200 with a chunked body, then a space every 100 ms, and never ends the body.
     *
     * <pre>
     * On main:
     * t(s)  0.0                         10.0
     * req 1 [=.=.=.=.=.=.=.=.=.=.=.=.=.=.=  ...   the body never ends, each chunk restarts the idle timer
     * the export never ends: the test gives up after 10 s and fails
     *
     * With the fix:
     * t(s)  0.0         1.0  ~1.1
     * req 1 [=.=.=.=.=.=X                  cancelled at its 1 s limit
     * req 2                  [=]           retry, answered at once
     * 2 requests and still 2 after 1.5 s more: passes
     * </pre>
     */
    @Test
    public void httpExportStalledOnABusyConnectionIsRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> {
            if (requests.incrementAndGet() == 1) {
                HttpServerResponse response = request.response().setChunked(true).setStatusCode(200);
                keepBusy(response, id -> response.write(" "));
            } else {
                request.response().setStatusCode(200).end();
            }
        });
        httpSender = httpSender();

        HttpResponse response = exportHttp();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertNoMoreRequests(requests, 2);
    }

    /**
     * A collector that receives the requests but never answers: every attempt times out, and once the retries are
     * used up the export fails instead of hanging.
     * <p>
     * This already works on main, where nothing is read so the idle timer fires. The test makes sure the fix keeps
     * the same outcome: 4 attempts (the first one and 3 retries), then the error callback.
     *
     * <pre>
     * On main and with the fix (each retry starts as soon as the previous attempt times out):
     * t(s)  0.0    1.0       ~2       ~3       ~4
     * req 1 [======X
     * req 2          [======X
     * req 3                   [======X
     * req 4                            [======X     no retry left: the export fails
     * 4 requests and an error: passes
     * </pre>
     */
    @Test
    public void grpcExportWithoutResponseFails() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> requests.incrementAndGet());
        grpcSender = grpcSender();

        assertThatThrownBy(this::exportGrpc).isInstanceOf(ExecutionException.class);
        assertThat(requests).hasValue(4);
    }

    /**
     * Same scenario as {@link #grpcExportWithoutResponseFails()}, over OTLP/HTTP.
     */
    @Test
    public void httpExportWithoutResponseFails() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        listen(request -> requests.incrementAndGet());
        httpSender = httpSender();

        assertThatThrownBy(this::exportHttp).isInstanceOf(ExecutionException.class);
        assertThat(requests).hasValue(4);
    }

    // The cancelled attempt must not start another retry once the export has completed
    private void assertNoMoreRequests(AtomicInteger requests, int expected) {
        await().during(Duration.ofMillis(1500)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(requests).hasValue(expected));
    }

    private void keepBusy(HttpServerResponse response, Handler<Long> traffic) {
        vertx.setPeriodic(100, id -> {
            if (response.closed()) {
                vertx.cancelTimer(id);
            } else {
                traffic.handle(id);
            }
        });
    }

    private void listen(Handler<HttpServerRequest> handler) throws Exception {
        collector = vertx.createHttpServer().requestHandler(handler);
        collector.listen(0, "localhost").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private URI collectorUri() {
        return URI.create("http://localhost:" + collector.actualPort());
    }

    private VertxGrpcSender grpcSender() {
        return new VertxGrpcSender(collectorUri(), VertxGrpcSender.GRPC_TRACE_SERVICE_NAME, false, TIMEOUT, Map.of(),
                options -> {
                }, vertx);
    }

    private VertxHttpSender httpSender() {
        return new VertxHttpSender(collectorUri(), VertxHttpSender.TRACES_PATH, false, TIMEOUT, Map.of(),
                "application/x-protobuf", options -> {
                }, vertx);
    }

    private void grpcOk(HttpServerResponse response) {
        response.putHeader("content-type", "application/grpc");
        // one empty message: no compression flag, zero length
        response.write(Buffer.buffer(new byte[5]));
        response.putTrailer("grpc-status", "0");
        response.end();
    }

    private GrpcResponse exportGrpc() throws Exception {
        CompletableFuture<GrpcResponse> result = new CompletableFuture<>();
        grpcSender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS); // safety limit for the test
    }

    private HttpResponse exportHttp() throws Exception {
        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        httpSender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS);
    }
}
