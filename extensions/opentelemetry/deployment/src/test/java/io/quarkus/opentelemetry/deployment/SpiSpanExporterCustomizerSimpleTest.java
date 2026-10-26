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
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With {@code quarkus.otel.simple}, a span exporter customizer registered through
 * {@link AutoConfigurationCustomizerProvider} receives the exporter of the simple processor on the declarative
 * configuration engine, and the spans reach that exporter.
 */
public class SpiSpanExporterCustomizerSimpleTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SpanExporterCustomizerProvider.class, TestSpanExporter.class,
                            TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(SpanExporterCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName())
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getName()),
                            "META-INF/services/" + ConfigurableSpanExporterProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.simple", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).as("the declarative engine is used").isEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void customizerReceivesTheExporterOfTheSimpleProcessor() {
        openTelemetry.getTracer("test").spanBuilder("simple-span").startSpan().end();

        // the simple processor exports on the calling thread; a batch processor would export later
        assertThat(testSpanExporter.getPartialFinishedSpanItems()).isNotEmpty();
        assertThat(SpanExporterCustomizerProvider.CUSTOMIZED).containsExactly("span");
        List<SpanData> spans = testSpanExporter.getFinishedSpanItems(1);
        assertThat(spans).extracting(SpanData::getName).containsExactly("simple-span");
    }

    public static class SpanExporterCustomizerProvider implements AutoConfigurationCustomizerProvider {

        static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addSpanExporterCustomizer((exporter, config) -> {
                if (exporter instanceof TestSpanExporter) {
                    CUSTOMIZED.add("span");
                }
                return exporter;
            });
        }
    }
}
