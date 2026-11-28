package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link Sampler} component named {@link QuarkusOTelModelBuilder#QUARKUS_CDI} to the application's
 * {@code Sampler} bean. The node's configuration is not used.
 */
public final class QuarkusCdiSamplerProvider implements ComponentProvider {

    private final Sampler sampler;

    /**
     * Creates the provider.
     *
     * @param sampler the sampler returned by {@link #create(DeclarativeConfigProperties)}, not {@code null}
     */
    public QuarkusCdiSamplerProvider(Sampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public Class<?> getType() {
        return Sampler.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_CDI;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return sampler;
    }
}
