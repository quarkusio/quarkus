package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.function.UnaryOperator;

import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeConfiguration;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link Sampler} component named {@link QuarkusOTelModelBuilder#QUARKUS_CUSTOMIZED}: builds the
 * sampler in the node's {@code delegate} entry and returns the result of the customizer applied to it.
 */
public final class QuarkusCustomizedSamplerProvider implements ComponentProvider {

    private final UnaryOperator<Sampler> customizer;

    /**
     * Creates the provider.
     *
     * @param customizer applied to the delegate sampler, not {@code null}
     */
    public QuarkusCustomizedSamplerProvider(UnaryOperator<Sampler> customizer) {
        this.customizer = customizer;
    }

    @Override
    public Class<?> getType() {
        return Sampler.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED;
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeclarativeConfigException if the node has no {@code delegate} entry
     */
    @Override
    public Object create(DeclarativeConfigProperties config) {
        DeclarativeConfigProperties delegate = config.getStructured("delegate");
        if (delegate == null) {
            throw new DeclarativeConfigException(
                    "The " + QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED + " sampler requires a delegate");
        }
        return customizer.apply(QuarkusDeclarativeConfiguration.createSampler(delegate));
    }
}
