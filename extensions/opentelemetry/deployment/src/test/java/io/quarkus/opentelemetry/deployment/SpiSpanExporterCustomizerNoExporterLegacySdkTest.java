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
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.tracing.DropAllSpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The {@code cdi} traces exporter has no {@code SpanExporter} beans, and an {@link AutoConfigurationCustomizerProvider}
 * customizes span exporters. Only after the customizer has run is it known whether spans are dropped, so the legacy
 * autoconfigure engine initializes the SDK.
 */
public class SpiSpanExporterCustomizerNoExporterLegacySdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SpanExporterCustomizerProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset(SpanExporterCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getMessage() != null && record.getMessage().contains("legacy autoconfigure engine"))
            .assertLogRecords(records -> assertThat(records).isNotEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void customizerReceivesTheNoopExporterAndSpansAreDropped() throws Exception {
        // forces the synthetic OpenTelemetry bean
        assertThat(openTelemetry.getTracer("test")).isNotNull();

        assertThat(SpanExporterCustomizerProvider.CUSTOMIZED).containsExactly("NoopSpanExporter");
        // The customizer returned the no-op exporter, so the batch processor around it was replaced
        assertThat(TestUtil.getActiveSpanProcessor(openTelemetry)).isInstanceOf(DropAllSpanProcessor.class);
    }

    public static class SpanExporterCustomizerProvider implements AutoConfigurationCustomizerProvider {

        static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addSpanExporterCustomizer((exporter, config) -> {
                CUSTOMIZED.add(exporter.getClass().getSimpleName());
                return exporter;
            });
        }
    }
}
