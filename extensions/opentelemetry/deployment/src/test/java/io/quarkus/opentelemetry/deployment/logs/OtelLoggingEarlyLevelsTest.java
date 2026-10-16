package io.quarkus.opentelemetry.deployment.logs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Enabling the OpenTelemetry log handler must not delay runtime logging setup: loggers must
 * already be at their configured levels while the OpenTelemetry SDK (and the CDI container it
 * needs) initializes. Otherwise every logger runs at the initial all-enabling level during the
 * rest of runtime initialization, triggering debug/trace-only code paths in libraries started
 * eagerly, see <a href="https://github.com/quarkusio/quarkus/issues/55889">GitHub issue #55889</a>.
 */
public class OtelLoggingEarlyLevelsTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SdkInitProbe.class)
                            .addClasses(InMemoryLogRecordExporter.class, InMemoryLogRecordExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.logs.enabled=true\n"),
                                    "application.properties"));

    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    @Test
    public void loggerLevelsAreConfiguredBeforeSdkInitialization() {
        assertTrue(SdkInitProbe.probed, "The probe customizer should have run during SDK initialization");
        assertFalse(SdkInitProbe.debugEnabledDuringSdkInit,
                "Loggers should already be at their configured levels while the OpenTelemetry SDK initializes; "
                        + "an arbitrary category must not be debug-enabled");
    }

    @Test
    public void recordsPublishedBeforeSdkIsReadyAreDelivered() {
        List<LogRecordData> records = logRecordExporter.getFinishedLogRecordItemsAtLeast(1);
        assertTrue(records.stream().anyMatch(r -> SdkInitProbe.EARLY_MESSAGE.equals(r.getBodyValue().asString())),
                "A record published while the SDK was initializing should be buffered and delivered");
    }

    @Singleton
    public static class SdkInitProbe implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

        static final String EARLY_MESSAGE = "Log record published while the OpenTelemetry SDK initializes";

        static volatile boolean probed;
        static volatile boolean debugEnabledDuringSdkInit;

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            // Runs while the OpenTelemetry SDK bean is created, before the SDK is available
            // to the log handler.
            debugEnabledDuringSdkInit = Logger.getLogger("org.acme.some.arbitrary.category").isLoggable(Level.FINE);
            probed = true;
            org.jboss.logging.Logger.getLogger("org.acme.early.logger").info(EARLY_MESSAGE);
        }
    }
}
