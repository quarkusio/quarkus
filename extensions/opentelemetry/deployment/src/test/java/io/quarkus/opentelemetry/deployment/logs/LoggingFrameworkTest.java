package io.quarkus.opentelemetry.deployment.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import io.quarkus.opentelemetry.deployment.common.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.InMemoryLogRecordExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class LoggingFrameworkTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JBossLoggingBean.class, SLF4JBean.class, JulBean.class, Log4j2Bean.class)
                            .addClasses(InMemoryLogRecordExporter.class, InMemoryLogRecordExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.logs.enabled=true\n" +
                                            "quarkus.otel.traces.enabled=false\n"),
                                    "application.properties"));

    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    @Inject
    JBossLoggingBean jBossLoggingBean;

    @Inject
    SLF4JBean slf4jBean;

    @Inject
    JulBean julBean;

    @Inject
    Log4j2Bean log4j2Bean;

    @BeforeEach
    void setup() {
        logRecordExporter.reset();
    }

    @Test
    public void testJBossLogging() {
        final String message = "JBoss Logging message";
        assertEquals("hello", jBossLoggingBean.hello(message));
        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);
        assertThat(last.getBody().asString()).isEqualTo(message);
    }

    @Test
    public void testSLF4JLogging() {
        final String message = "SLF4J Logging message";
        assertEquals("hello", slf4jBean.hello(message));
        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);
        assertThat(last.getBody().asString()).isEqualTo(message);
    }

    @Test
    public void testLog4jLogging() {
        final String message = "Log4j Logging message";
        assertEquals("hello", log4j2Bean.hello(message));
        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);
        assertThat(last.getBody().asString()).isEqualTo(message);
    }

    @Test
    public void testJulLogging() {
        final String message = "JUL Logging message";
        assertEquals("hello", julBean.hello(message));
        List<LogRecordData> finishedLogRecordItems = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        LogRecordData last = finishedLogRecordItems.get(finishedLogRecordItems.size() - 1);
        assertThat(last.getBody().asString()).isEqualTo(message);
    }

    @ApplicationScoped
    public static class JBossLoggingBean {
        private static final Logger LOG = Logger.getLogger(JBossLoggingBean.class.getName());

        public String hello(final String message) {
            LOG.info(message);
            return "hello";
        }
    }

    @ApplicationScoped
    public static class SLF4JBean {
        // using the logger adapter: https://quarkus.io/guides/logging#add-a-logging-adapter-to-your-application
        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SLF4JBean.class);

        public String hello(final String message) {
            LOG.info(message);
            return "hello";
        }
    }

    @ApplicationScoped
    public static class Log4j2Bean {
        // using the logger adapter: https://quarkus.io/guides/logging#add-a-logging-adapter-to-your-application
        private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager
                .getLogger(Log4j2Bean.class);

        public String hello(final String message) {
            LOG.info(message);
            return "hello";
        }
    }

    @ApplicationScoped
    public static class JulBean {
        private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(JulBean.class.getName());

        public String hello(final String message) {
            LOG.info(message);
            return "hello";
        }
    }

}
