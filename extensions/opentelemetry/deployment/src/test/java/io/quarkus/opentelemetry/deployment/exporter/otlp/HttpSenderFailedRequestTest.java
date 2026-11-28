package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.opentelemetry.sdk.common.export.HttpResponse;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;

/**
 * An export whose request cannot be executed must fail, and the warning must name the exported signal. A custom signal
 * path must be accepted too.
 */
public class HttpSenderFailedRequestTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class))
            // the exporters of the application would log the same warning, nothing listens on the default endpoint
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .setLogRecordPredicate(record -> VertxHttpSender.class.getName().equals(record.getLoggerName())
                    && record.getMessage().contains("The request could not be executed"))
            .assertLogRecords(logRecords -> assertThat(logRecords)
                    .allSatisfy(logRecord -> assertThat(logRecord.getLevel()).isEqualTo(Level.WARNING))
                    .extracting(LogRecord::getMessage)
                    .satisfiesExactlyInAnyOrder(
                            message -> assertThat(message).startsWith("Failed to export spans."),
                            message -> assertThat(message).startsWith("Failed to export metrics."),
                            message -> assertThat(message).startsWith("Failed to export logs."),
                            message -> assertThat(message).startsWith("Failed to export telemetry data.")));

    @Inject
    Vertx vertx;

    private VertxHttpSender sender;

    @AfterEach
    void cleanup() {
        if (sender != null) {
            sender.shutdown();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { VertxHttpSender.TRACES_PATH, VertxHttpSender.METRICS_PATH, VertxHttpSender.LOGS_PATH,
            "/custom" })
    void exportToUnreachableCollectorFails(String signalPath) throws Exception {
        // Nothing listens on the port, so every attempt is refused
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        sender = new VertxHttpSender(URI.create("http://localhost:" + port), signalPath, false,
                Duration.ofSeconds(5), Map.of(), "application/x-protobuf", options -> {
                }, vertx);

        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        sender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);

        assertThatThrownBy(() -> result.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
    }
}
