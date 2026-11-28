package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Each propagator goes through the {@link TextMapPropagatorCustomizer} beans first and the propagator customizers of
 * {@link AutoConfigurationCustomizerProvider} implementations last, as on the autoconfigure path, which ran the
 * providers after the Quarkus customizer beans had registered.
 */
public class SpiPropagatorCustomizerOrderTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(QuarkusCustomizer.class, SpiCustomizerProvider.class)
                    .addAsResource(new StringAsset(SpiCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.propagators", "tracecontext,baggage")
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).as("the declarative engine is used").isEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void quarkusCustomizersRunBeforeSpiCustomizers() {
        assertThat(openTelemetry.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "baggage");

        assertThat(CALLS).containsExactly(
                "quarkus:W3CTraceContextPropagator", "spi:W3CTraceContextPropagator",
                "quarkus:W3CBaggagePropagator", "spi:W3CBaggagePropagator");
    }

    @Singleton
    public static class QuarkusCustomizer implements TextMapPropagatorCustomizer {
        @Override
        public TextMapPropagator customize(Context context) {
            CALLS.add("quarkus:" + context.propagator().getClass().getSimpleName());
            return context.propagator();
        }
    }

    public static class SpiCustomizerProvider implements AutoConfigurationCustomizerProvider {
        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addPropagatorCustomizer((propagator, config) -> {
                CALLS.add("spi:" + propagator.getClass().getSimpleName());
                return propagator;
            });
        }
    }
}
