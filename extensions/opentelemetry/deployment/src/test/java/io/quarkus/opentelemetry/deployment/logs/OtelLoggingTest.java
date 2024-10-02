package io.quarkus.opentelemetry.deployment.logs;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_LINENO;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OtelLoggingTest {

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
    public void testLoggingData() {
        final String message = "Logging message to test the different logging attributes";
        assertEquals("hello", jBossLoggingBean.hello(message));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        assertThat(last.getSpanContext().getSpanId()).isEqualTo("0000000000000000");
        assertThat(last.getSpanContext().getTraceId()).isEqualTo("00000000000000000000000000000000");
        assertThat(last.getSpanContext().getTraceFlags().asHex()).isEqualTo("00");
        assertThat(last.getTimestampEpochNanos()).isNotNull().isLessThan(System.currentTimeMillis() * 1_000_000);

        assertThat(last)
                .hasSeverity(Severity.INFO)
                .hasSeverityText("INFO")
                .hasBody(message)
                .hasAttributesSatisfying(
                        attributes -> assertThat(attributes)
                                .containsEntry(CODE_NAMESPACE.getKey(),
                                        "io.quarkus.opentelemetry.deployment.logs.OtelLoggingTest$JBossLoggingBean")
                                .containsEntry(CODE_FUNCTION.getKey(), "hello")
                                .containsEntry(THREAD_NAME.getKey(), Thread.currentThread().getName())
                                .containsEntry(THREAD_ID.getKey(), Thread.currentThread().getId())
                                .containsEntry("log.logger.namespace", "org.jboss.logging.Logger")
                                .containsKey(CODE_LINENO.getKey())
                                .doesNotContainKey(EXCEPTION_TYPE)
                                .doesNotContainKey(EXCEPTION_MESSAGE)
                                .doesNotContainKey(EXCEPTION_STACKTRACE)
                                .doesNotContainKey(LOG_FILE_PATH)
                                // attributed do not duplicate tracing data
                                .doesNotContainKey("spanId")
                                .doesNotContainKey("traceId")
                                .doesNotContainKey("sampled"));
    }

    @Test
    public void testTrace() {
        final String message = "Logging with tracing";
        assertEquals("hello", jBossLoggingBean.helloTraced(message));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        final SpanData span = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");

        assertThat(span.getName()).isEqualTo("JBossLoggingBean.helloTraced");
        assertThat(last.getSpanContext().getSpanId()).isEqualTo(span.getSpanId());
        assertThat(last.getSpanContext().getTraceId()).isEqualTo(span.getTraceId());
        assertThat(last.getSpanContext().getTraceFlags()).isEqualTo(span.getSpanContext().getTraceFlags());

        assertThat(last)
                .hasSeverity(Severity.INFO)
                .hasSeverityText("INFO")
                .hasBody(message)
                .hasAttributesSatisfying(
                        attributes -> assertThat(attributes)
                                .containsEntry(CODE_NAMESPACE.getKey(),
                                        "io.quarkus.opentelemetry.deployment.logs.OtelLoggingTest$JBossLoggingBean")
                                .containsEntry(CODE_FUNCTION.getKey(), "helloTraced")
                                .containsEntry(THREAD_NAME.getKey(), Thread.currentThread().getName())
                                .containsEntry(THREAD_ID.getKey(), Thread.currentThread().getId())
                                .containsEntry("log.logger.namespace", "org.jboss.logging.Logger")
                                .containsKey(CODE_LINENO.getKey())
                                .doesNotContainKey(EXCEPTION_TYPE)
                                .doesNotContainKey(EXCEPTION_MESSAGE)
                                .doesNotContainKey(EXCEPTION_STACKTRACE)
                                .doesNotContainKey(LOG_FILE_PATH)
                                // attributed do not duplicate tracing data
                                .doesNotContainKey("spanId")
                                .doesNotContainKey("traceId")
                                .doesNotContainKey("sampled"));
    }

    @Test
    public void testException() {
        final Throwable craftedException = new RuntimeException("Crafted exception");
        assertTrue(jBossLoggingBean.logException(craftedException));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        assertThat(last)
                .hasSeverity(Severity.ERROR)
                .hasSeverityText("ERROR")
                .hasAttributesSatisfying(
                        attributes -> assertThat(attributes)
                                .containsEntry("log.logger.namespace", "org.jboss.logging.Logger")
                                .containsEntry(EXCEPTION_TYPE, "java.lang.RuntimeException")
                                .containsEntry(EXCEPTION_MESSAGE, "Crafted exception")
                                .containsEntry(EXCEPTION_STACKTRACE, extractStackTrace(craftedException))
                                .doesNotContainKey(LOG_FILE_PATH)
                                // attributed do not duplicate tracing data
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

        @WithSpan
        public String helloTraced(final String message) {
            LOG.info(message);
            return "hello";
        }

        public boolean logException(final Throwable throwable) {
            LOG.error("logging an exception", throwable);
            return true;
        }
    }
}
