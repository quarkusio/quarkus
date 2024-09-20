package io.quarkus.opentelemetry.deployment.logs;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_LINENO;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import io.opentelemetry.sdk.logs.data.LogRecordData;
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

        assertThat(last.getTimestampEpochNanos()).isNotNull().isLessThan(System.currentTimeMillis() * 1_000_000);
        assertThat(last.getSeverityText()).isEqualTo("INFO");
        assertThat(last.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(last.getBody().asString()).isEqualTo(message);

        Map<AttributeKey<?>, Object> attributesMap = last.getAttributes().asMap();
        assertThat(attributesMap.get(CODE_NAMESPACE))
                .isEqualTo("io.quarkus.opentelemetry.deployment.logs.OtelLoggingFileTest$JBossLoggingBean");
        assertThat(attributesMap.get(CODE_FUNCTION)).isEqualTo("hello");
        assertThat((Long) attributesMap.get(CODE_LINENO)).isGreaterThan(0);
        assertThat(attributesMap.get(THREAD_NAME)).isEqualTo(Thread.currentThread().getName());
        assertThat(attributesMap.get(THREAD_ID)).isEqualTo(Thread.currentThread().getId());
        assertThat(attributesMap.get(AttributeKey.stringKey("log.logger.namespace"))).isEqualTo("org.jboss.logging.Logger");
        assertThat(attributesMap.get(EXCEPTION_TYPE)).isNull();
        // using the default location for the log file
        assertThat(attributesMap.get(LOG_FILE_PATH)).isEqualTo("target/quarkus.log");
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
