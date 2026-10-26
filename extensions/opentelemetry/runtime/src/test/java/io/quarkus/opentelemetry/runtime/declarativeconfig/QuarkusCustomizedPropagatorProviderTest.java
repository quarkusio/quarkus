package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCustomizedPropagatorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;

class QuarkusCustomizedPropagatorProviderTest {

    @Test
    void builtInPropagatorsAreCustomized() {
        List<TextMapPropagator> received = new ArrayList<>();
        TextMapPropagator replacement = TextMapPropagator.noop();

        Object propagator = new QuarkusCustomizedPropagatorProvider(recording(received, replacement))
                .create(node(loader(), "tracecontext"));

        assertThat(propagator).isSameAs(replacement);
        assertThat(received).containsExactly(W3CTraceContextPropagator.getInstance());
    }

    @Test
    void otherNamesResolveThroughTheComponentLoader() {
        TextMapPropagator custom = new FieldPropagator("custom-field");
        List<TextMapPropagator> received = new ArrayList<>();

        Object propagator = new QuarkusCustomizedPropagatorProvider(recording(received, null))
                .create(node(loader(provider("custom", custom)), "custom"));

        assertThat(propagator).isSameAs(custom);
        assertThat(received).containsExactly(custom);
    }

    @Test
    void anUnknownNameFails() {
        assertThatThrownBy(() -> new QuarkusCustomizedPropagatorProvider(UnaryOperator.identity())
                .create(node(loader(), "missing")))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("No TextMapPropagator named 'missing'");
    }

    @Test
    void theNameIsRequired() {
        assertThatThrownBy(() -> new QuarkusCustomizedPropagatorProvider(UnaryOperator.identity())
                .create(YamlDeclarativeConfigProperties.create(Map.of(), loader())))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("name");
    }

    @Test
    void eachModelPropagatorIsCustomized() {
        List<TextMapPropagator> received = new ArrayList<>();
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"))
                .propagatorsCustomized(true)
                .build();

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(
                        new QuarkusCustomizedPropagatorProvider(recording(received, null))),
                InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            // the default propagators, customized individually
            assertThat(received).containsExactly(W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance());
            assertThat(sdk.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "baggage");
        }
    }

    // A quarkus_customized propagator node for name
    private static DeclarativeConfigProperties node(ComponentLoader loader, String name) {
        return YamlDeclarativeConfigProperties.create(Map.of("name", name), loader);
    }

    // Records each propagator and returns result, or the propagator itself when result is null
    private static UnaryOperator<TextMapPropagator> recording(List<TextMapPropagator> received,
            TextMapPropagator result) {
        return propagator -> {
            received.add(propagator);
            return result == null ? propagator : result;
        };
    }

    private static ComponentProvider provider(String name, TextMapPropagator propagator) {
        return new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return TextMapPropagator.class;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                return propagator;
            }
        };
    }

    // providers and the drop-all provider, which the module's service file registers
    private static ComponentLoader loader(ComponentProvider... providers) {
        List<ComponentProvider> all = new ArrayList<>(List.of(providers));
        all.add(new QuarkusDropAllSpanProcessorProvider());
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == ComponentProvider.class ? (Iterable<T>) all : List.of();
            }
        };
    }

    private record FieldPropagator(String field) implements TextMapPropagator {

        @Override
        public Collection<String> fields() {
            return List.of(field);
        }

        @Override
        public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        }

        @Override
        public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
            return context;
        }
    }
}
