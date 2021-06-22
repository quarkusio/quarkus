package io.quarkus.opentelemetry.runtime;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

public final class OpenTelemetryUtil {
    private OpenTelemetryUtil() {
    }

    private static TextMapPropagator getPropagator(
            String name, Map<String, TextMapPropagator> spiPropagators) {
        if ("tracecontext".equals(name)) {
            return W3CTraceContextPropagator.getInstance();
        }
        if ("baggage".equals(name)) {
            return W3CBaggagePropagator.getInstance();
        }

        TextMapPropagator spiPropagator = spiPropagators.get(name);
        if (spiPropagator != null) {
            return spiPropagator;
        }
        throw new IllegalArgumentException(
                "Unrecognized value for propagator: " + name
                        + ". Make sure the artifact including the propagator is on the classpath.");
    }

    public static ContextPropagators mapPropagators(List<String> propagators) {
        Map<String, TextMapPropagator> spiPropagators = StreamSupport.stream(
                ServiceLoader.load(ConfigurablePropagatorProvider.class).spliterator(), false)
                .collect(
                        Collectors.toMap(ConfigurablePropagatorProvider::getName,
                                ConfigurablePropagatorProvider::getPropagator));

        Set<TextMapPropagator> selectedPropagators = propagators.stream()
                .map(propagator -> getPropagator(propagator.trim(), spiPropagators))
                .collect(Collectors.toSet());

        return ContextPropagators.create(TextMapPropagator.composite(selectedPropagators));
    }
}
