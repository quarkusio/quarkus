package io.quarkus.opentelemetry.runtime.propagation;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * /**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the {@link TextMapPropagator}
 * that are to be registered with OpenTelemetry
 */
public interface TextMapPropagatorCustomizer {

    TextMapPropagator customize(Context context);

    interface Context {
        TextMapPropagator propagator();

        ConfigProperties otelConfigProperties();
    }
}
