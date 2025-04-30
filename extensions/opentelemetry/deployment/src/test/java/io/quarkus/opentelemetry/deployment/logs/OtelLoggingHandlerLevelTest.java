package io.quarkus.opentelemetry.deployment.logs;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OtelLoggingHandlerLevelTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JBossLoggingBean.class)
                            .addClasses(InMemoryLogRecordExporter.class, InMemoryLogRecordExporterProvider.class,
                                    TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider")
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.logs.enabled=true\n" +
                                            "quarkus.otel.logs.level=ERROR\n" +
                                            "quarkus.otel.traces.enabled=true\n"),
                                    "application.properties"));

    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    JBossLoggingBean jBossLoggingBean;

    @BeforeEach
    void setup() {
        logRecordExporter.reset();
        spanExporter.reset();
    }

    @Test
    public void testINFONoShow() {
        final String message = "Info message must not be in the logs";
        RuntimeException craftedException = new RuntimeException("crafted exception");

        assertEquals("hello", jBossLoggingBean.hello(message)); // INFO level suppressed
        assertEquals(Boolean.TRUE, jBossLoggingBean.logException(craftedException));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);

        // There should be only one log record, the error log
        assertThat(finishedLogRecordItems.size())
                .withFailMessage(() -> finishedLogRecordItems.stream()
                        .map(LogRecordData::getBodyValue)
                        .map(line -> line.asString() + "\n")
                        .collect(toList())
                        .toString())
                .isOne();

        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        assertThat(last.getSpanContext().getSpanId()).isEqualTo("0000000000000000");
        assertThat(last.getSpanContext().getTraceId()).isEqualTo("00000000000000000000000000000000");
        assertThat(last.getSpanContext().getTraceFlags().asHex()).isEqualTo("00");
        assertThat(last.getTimestampEpochNanos()).isNotNull().isLessThan(System.currentTimeMillis() * 1_000_000);
        assertThat(last)
                .hasSeverity(Severity.ERROR)
                .hasSeverityText("ERROR")
                .hasAttributesSatisfying(
                        attributes -> assertThat(attributes)
                                .containsEntry("log.logger.namespace", "org.jboss.logging.Logger")
                                .containsEntry(EXCEPTION_TYPE, "java.lang.RuntimeException")
                                .containsEntry(EXCEPTION_MESSAGE, "crafted exception")
                                .containsEntry(EXCEPTION_STACKTRACE, extractStackTrace(craftedException))
                                .doesNotContainKey(LOG_FILE_PATH)
                                // attributes do not duplicate tracing data
                                .doesNotContainKey("spanId")
                                .doesNotContainKey("traceId")
                                .doesNotContainKey("sampled"));
    }

    private String extractStackTrace(final Throwable throwable) {
        try (StringWriter sw = new StringWriter(1024); PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            sw.flush();
            return sw.toString();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @ApplicationScoped
    public static class JBossLoggingBean {
        private static final Logger LOG = Logger.getLogger(JBossLoggingBean.class.getName());

        public String hello(final String message) {
            LOG.info(message);
            return "hello";
        }

        public boolean logException(final Throwable throwable) {
            LOG.error("logging an exception", throwable);
            return true;
        }
    }
}
