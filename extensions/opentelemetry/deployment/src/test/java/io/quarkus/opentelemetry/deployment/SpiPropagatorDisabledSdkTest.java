package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A disabled SDK keeps the configured propagators, customized by the {@link TextMapPropagatorCustomizer} beans, as
 * autoconfigure configured them before it checked {@code otel.sdk.disabled}. A {@link ConfigurablePropagatorProvider}
 * that implements {@link AutoConfigureListener} receives the SDK that is used.
 */
public class SpiPropagatorDisabledSdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RecordingCustomizer.class, ListeningPropagatorProvider.class, FieldPropagator.class)
                    .addAsResource(new StringAsset(ListeningPropagatorProvider.class.getName()),
                            "META-INF/services/" + ConfigurablePropagatorProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true")
            .overrideConfigKey("quarkus.otel.propagators", "tracecontext,listening")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void propagatorsAreCustomizedAndTheListenerReceivesTheSdk() {
        assertThat(openTelemetry.getPropagators().getTextMapPropagator().fields())
                .contains("traceparent", "listening-field");

        assertThat(RecordingCustomizer.CUSTOMIZED).containsExactly("W3CTraceContextPropagator", "FieldPropagator");
        assertThat(ListeningPropagatorProvider.SDK.get()).isNotNull();
        assertThat(ListeningPropagatorProvider.SDK.get().getPropagators()).isSameAs(openTelemetry.getPropagators());
    }

    @Singleton
    public static class RecordingCustomizer implements TextMapPropagatorCustomizer {

        static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

        @Override
        public TextMapPropagator customize(Context context) {
            CUSTOMIZED.add(context.propagator().getClass().getSimpleName());
            return context.propagator();
        }
    }

    public static class ListeningPropagatorProvider implements ConfigurablePropagatorProvider, AutoConfigureListener {

        static final AtomicReference<OpenTelemetrySdk> SDK = new AtomicReference<>();

        @Override
        public TextMapPropagator getPropagator(ConfigProperties config) {
            return new FieldPropagator();
        }

        @Override
        public String getName() {
            return "listening";
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            SDK.set(sdk);
        }
    }

    public static class FieldPropagator implements TextMapPropagator {

        @Override
        public Collection<String> fields() {
            return List.of("listening-field");
        }

        @Override
        public <C> void inject(io.opentelemetry.context.Context context, C carrier, TextMapSetter<C> setter) {
        }

        @Override
        public <C> io.opentelemetry.context.Context extract(io.opentelemetry.context.Context context, C carrier,
                TextMapGetter<C> getter) {
            return context;
        }
    }
}
