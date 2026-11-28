package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.function.UnaryOperator;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link TextMapPropagator} component named {@link QuarkusOTelModelBuilder#QUARKUS_CUSTOMIZED}: builds
 * the propagator the node's {@code name} entry names and returns the result of the customizer applied to it. The
 * names {@code tracecontext} and {@code baggage} are the W3C propagators; any other name is resolved through the
 * {@link ComponentProvider}s of the component loader, as upstream resolves a propagator name.
 */
public final class QuarkusCustomizedPropagatorProvider implements ComponentProvider {

    private final UnaryOperator<TextMapPropagator> customizer;

    /**
     * Creates the provider.
     *
     * @param customizer applied to each propagator, not {@code null}
     */
    public QuarkusCustomizedPropagatorProvider(UnaryOperator<TextMapPropagator> customizer) {
        this.customizer = customizer;
    }

    @Override
    public Class<?> getType() {
        return TextMapPropagator.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED;
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeclarativeConfigException if the node has no {@code name} entry, or no propagator has that name
     */
    @Override
    public Object create(DeclarativeConfigProperties config) {
        String name = config.getString("name");
        if (name == null) {
            throw new DeclarativeConfigException(
                    "The " + QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED + " propagator requires a name");
        }
        return create(name, config.getComponentLoader(), config);
    }

    /**
     * Returns the customized propagator {@code name} names.
     *
     * @param name the propagator name, as {@code otel.propagators} lists it, not {@code null}
     * @param componentLoader resolves the names other than {@code tracecontext} and {@code baggage}, not {@code null}
     * @return the result of the customizer applied to the propagator
     * @throws DeclarativeConfigException if no propagator has the name
     */
    public TextMapPropagator create(String name, ComponentLoader componentLoader) {
        return create(name, componentLoader, DeclarativeConfigProperties.empty());
    }

    // parent: the node whose ConfigProvider the resolved propagator receives
    private TextMapPropagator create(String name, ComponentLoader componentLoader, DeclarativeConfigProperties parent) {
        TextMapPropagator propagator = switch (name) {
            case "tracecontext" -> W3CTraceContextPropagator.getInstance();
            case "baggage" -> W3CBaggagePropagator.getInstance();
            // As upstream resolves a propagator name: with empty configuration
            default -> (TextMapPropagator) Components.create(componentLoader, TextMapPropagator.class, name,
                    Components.config(parent, DeclarativeConfigProperties.empty(), name));
        };
        if (propagator == null) {
            throw Components.noSuchComponent(TextMapPropagator.class, name);
        }
        return customizer.apply(propagator);
    }
}
