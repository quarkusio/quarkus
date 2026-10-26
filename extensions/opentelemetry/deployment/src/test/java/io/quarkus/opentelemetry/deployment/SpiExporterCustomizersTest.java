package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Span and log record exporter customizers registered through {@link AutoConfigurationCustomizerProvider} receive
 * the configured exporters on the declarative configuration engine, as they did on the autoconfigure path.
 */
public class SpiExporterCustomizersTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExporterCustomizerProvider.class, TestSpanExporter.class, TestSpanExporterProvider.class,
                            InMemoryLogRecordExporter.class, InMemoryLogRecordExporterProvider.class)
                    .addAsResource(new StringAsset(ExporterCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName())
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getName()),
                            "META-INF/services/" + ConfigurableSpanExporterProvider.class.getName())
                    .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getName()),
                            "META-INF/services/" + ConfigurableLogRecordExporterProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.exporter", "in-memory")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).as("the declarative engine is used").isEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void customizersReceiveTheExporters() {
        // forces the synthetic OpenTelemetry bean
        assertThat(openTelemetry.getTracer("test")).isNotNull();

        assertThat(ExporterCustomizerProvider.CUSTOMIZED).containsExactlyInAnyOrder("span", "log");
    }

    public static class ExporterCustomizerProvider implements AutoConfigurationCustomizerProvider {

        static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addSpanExporterCustomizer((exporter, config) -> {
                if (exporter instanceof TestSpanExporter) {
                    CUSTOMIZED.add("span");
                }
                return exporter;
            });
            autoConfiguration.addLogRecordExporterCustomizer((exporter, config) -> {
                if (exporter instanceof InMemoryLogRecordExporter) {
                    CUSTOMIZED.add("log");
                }
                return exporter;
            });
        }
    }
}
