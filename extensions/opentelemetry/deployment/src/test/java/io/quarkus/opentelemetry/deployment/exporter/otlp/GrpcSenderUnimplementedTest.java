package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * A collector without a pipeline for a signal answers the exports of that signal with UNIMPLEMENTED. The export must
 * fail, so that the next one can start, and the warning for this case must be logged once.
 */
public class GrpcSenderUnimplementedTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class))
            .setLogRecordPredicate(record -> VertxGrpcSender.class.getName().equals(record.getLoggerName())
                    && record.getMessage().contains("UNIMPLEMENTED"))
            .assertLogRecords(logRecords -> assertThat(logRecords)
                    .allSatisfy(logRecord -> assertThat(logRecord.getLevel()).isEqualTo(Level.WARNING))
                    .extracting(LogRecord::getMessage)
                    .satisfiesExactlyInAnyOrder(
                            message -> assertThat(message).startsWith("Failed to export spans.")
                                    .contains("OTEL_TRACES_EXPORTER=none", "unknown service"),
                            message -> assertThat(message).startsWith("Failed to export metrics.")
                                    .contains("OTEL_METRICS_EXPORTER=none", "unknown service"),
                            message -> assertThat(message).startsWith("Failed to export logs.")
                                    .contains("OTEL_LOGS_EXPORTER=none", "unknown service")));

    @Inject
    Vertx vertx;

    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer collector;
    private VertxGrpcSender grpcSender;

    @BeforeEach
    void startCollector() throws Exception {
        requests.set(0);
        collector = vertx.createHttpServer().requestHandler(request -> {
            requests.incrementAndGet();
            // a trailers-only response, as sent by a collector that has no pipeline for the signal
            request.response()
                    .putHeader("content-type", "application/grpc")
                    .putHeader("grpc-status", "12")
                    .putHeader("grpc-message", "unknown service")
                    .end();
        });
        collector.listen(0, "localhost").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (grpcSender != null) {
            grpcSender.shutdown();
        }
        collector.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @ValueSource(strings = { VertxGrpcSender.GRPC_TRACE_SERVICE_NAME, VertxGrpcSender.GRPC_METRIC_SERVICE_NAME,
            VertxGrpcSender.GRPC_LOG_SERVICE_NAME })
    void exportAnsweredWithUnimplementedFails(String grpcServiceName) {
        grpcSender = new VertxGrpcSender(URI.create("http://localhost:" + collector.actualPort()), grpcServiceName, false,
                Duration.ofSeconds(1), Map.of(), options -> {
                }, vertx);

        // every export has to complete, the periodic metric reader does not start one while another is in flight,
        // but the warning is only logged for the first one, see the log record assertion
        assertThatThrownBy(this::export).isInstanceOf(ExecutionException.class);
        assertThatThrownBy(this::export).isInstanceOf(ExecutionException.class);
        assertThat(requests).hasValue(2);
    }

    private void export() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        grpcSender.send(new EmptyMessageWriter(), response -> result.complete(null), result::completeExceptionally);
        result.get(10, TimeUnit.SECONDS); // safety limit for the test
    }
}
