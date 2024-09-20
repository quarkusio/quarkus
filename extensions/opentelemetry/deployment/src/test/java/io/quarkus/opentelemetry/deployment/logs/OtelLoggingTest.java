package io.quarkus.opentelemetry.deployment.logs;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
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
        assertThat(last.getSeverityText()).isEqualTo("INFO");
        assertThat(last.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(last.getBody().asString()).isEqualTo(message);

        Map<AttributeKey<?>, Object> attributesMap = last.getAttributes().asMap();
        assertThat(attributesMap.get(CODE_NAMESPACE))
                .isEqualTo("io.quarkus.opentelemetry.deployment.logs.OtelLoggingTest$JBossLoggingBean");
        assertThat(attributesMap.get(CODE_FUNCTION)).isEqualTo("hello");
        assertThat((Long) attributesMap.get(CODE_LINENO)).isGreaterThan(0);
        assertThat(attributesMap.get(THREAD_NAME)).isEqualTo(Thread.currentThread().getName());
        assertThat(attributesMap.get(THREAD_ID)).isEqualTo(Thread.currentThread().getId());
        assertThat(attributesMap.get(AttributeKey.stringKey("log.logger.namespace"))).isEqualTo("org.jboss.logging.Logger");
        assertThat(attributesMap.get(EXCEPTION_TYPE)).isNull();
        assertThat(attributesMap.get(EXCEPTION_MESSAGE)).isNull();
        assertThat(attributesMap.get(EXCEPTION_STACKTRACE)).isNull();
        assertThat(attributesMap.get(LOG_FILE_PATH)).isNull();
        // attributed do not duplicate tracing data
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("spanId"))).isFalse();
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("traceId"))).isFalse();
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("sampled"))).isFalse();
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
        assertThat(last.getBody().asString()).isEqualTo(message);

        Map<AttributeKey<?>, Object> attributesMap = last.getAttributes().asMap();
        assertThat(attributesMap.get(CODE_NAMESPACE))
                .isEqualTo("io.quarkus.opentelemetry.deployment.logs.OtelLoggingTest$JBossLoggingBean");
        assertThat(attributesMap.get(CODE_FUNCTION)).isEqualTo("helloTraced");
        // attributed do not duplicate tracing data
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("spanId"))).isFalse();
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("traceId"))).isFalse();
        assertThat(attributesMap.keySet().contains(AttributeKey.stringKey("sampled"))).isFalse();
    }

    @Test
    public void testException() {
        final Throwable craftedException = new RuntimeException("Crafted exception");
        assertTrue(jBossLoggingBean.logException(craftedException));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        assertThat(last.getSeverityText()).isEqualTo("ERROR");
        assertThat(last.getSeverity()).isEqualTo(Severity.ERROR);
        Map<AttributeKey<?>, Object> attributesMap = last.getAttributes().asMap();
        assertThat(attributesMap.get(EXCEPTION_TYPE)).isEqualTo("java.lang.RuntimeException");
        assertThat(attributesMap.get(EXCEPTION_MESSAGE)).isEqualTo("Crafted exception");
        assertThat(attributesMap.get(EXCEPTION_STACKTRACE)).isEqualTo(extractStackTrace(craftedException));
        assertThat(attributesMap.get(LOG_FILE_PATH)).isNull();
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
