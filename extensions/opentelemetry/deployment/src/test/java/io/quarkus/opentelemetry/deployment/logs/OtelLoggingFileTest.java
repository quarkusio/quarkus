package io.quarkus.opentelemetry.deployment.logs;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_LINENO;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
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
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OtelLoggingFileTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JBossLoggingBean.class)
                            .addClasses(InMemoryLogRecordExporter.class, InMemoryLogRecordExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.logs.enabled=true\n" +
                                            "quarkus.log.file.enable=true\n" + // enable log file
                                            "quarkus.otel.traces.enabled=false\n"),
                                    "application.properties"));

    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    @Inject
    JBossLoggingBean jBossLoggingBean;

    @BeforeEach
    void setup() {
        logRecordExporter.reset();
    }

    @Test
    public void testLoggingData() {
        final String message = "Logging message to test the different logging attributes";
        assertEquals("hello", jBossLoggingBean.hello(message));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);

        OpenTelemetryAssertions.assertThat(last)
                .hasSeverity(Severity.INFO)
                .hasSeverityText("INFO")
                .hasBody(message)
                .hasAttributesSatisfying(
                        attributes -> OpenTelemetryAssertions.assertThat(attributes)
                                .containsEntry(CODE_NAMESPACE.getKey(),
                                        "io.quarkus.opentelemetry.deployment.logs.OtelLoggingFileTest$JBossLoggingBean")
                                .containsEntry(CODE_FUNCTION.getKey(), "hello")
                                .containsEntry(THREAD_NAME.getKey(), Thread.currentThread().getName())
                                .containsEntry(THREAD_ID.getKey(), Thread.currentThread().getId())
                                .containsEntry("log.logger.namespace", "org.jboss.logging.Logger")
                                .containsEntry(LOG_FILE_PATH, "target" + File.separator + "quarkus.log")
                                .containsKey(CODE_LINENO.getKey())
                                .doesNotContainKey(EXCEPTION_TYPE)
                                .doesNotContainKey(EXCEPTION_MESSAGE)
                                .doesNotContainKey(EXCEPTION_STACKTRACE)
                                // attributed do not duplicate tracing data
                                .doesNotContainKey("spanId")
                                .doesNotContainKey("traceId")
                                .doesNotContainKey("sampled"));
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
