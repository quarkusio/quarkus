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
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A propagator customizer registered through {@link AutoConfigurationCustomizerProvider}, with no
 * {@code TextMapPropagatorCustomizer} beans, receives each configured propagator, as on the autoconfigure path.
 */
public class SpiPropagatorCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SpiCustomizerProvider.class)
                    .addAsResource(new StringAsset(SpiCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.otel.propagators", "tracecontext,baggage")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).as("the declarative engine is used").isEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void spiCustomizerReceivesEachPropagator() {
        assertThat(openTelemetry.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "baggage");

        assertThat(SpiCustomizerProvider.CUSTOMIZED).containsExactly("W3CTraceContextPropagator", "W3CBaggagePropagator");
    }

    public static class SpiCustomizerProvider implements AutoConfigurationCustomizerProvider {

        static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addPropagatorCustomizer((propagator, config) -> {
                CUSTOMIZED.add(propagator.getClass().getSimpleName());
                return propagator;
            });
        }
    }
}
