package io.quarkus.opentelemetry.deployment.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
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

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OtelLogsHandlerDisabledTest {
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
                                            "quarkus.otel.logs.handler.enabled=false\n" +
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
        final String message = "Logging handler disabled";
        assertEquals("hello", jBossLoggingBean.hello(message));

        List<LogRecordData> finishedLogRecordItems = logRecordExporter
                .getFinishedLogRecordItemsWithWait(Duration.ofMillis(1000));
        assertEquals(0, finishedLogRecordItems.size());
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
