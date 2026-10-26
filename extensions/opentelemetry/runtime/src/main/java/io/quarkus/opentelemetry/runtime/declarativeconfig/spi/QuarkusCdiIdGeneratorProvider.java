package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link IdGenerator} component named {@link QuarkusOTelModelBuilder#QUARKUS_CDI} to the application's
 * {@code IdGenerator} bean. The node's configuration is not used.
 */
public final class QuarkusCdiIdGeneratorProvider implements ComponentProvider {

    private final IdGenerator idGenerator;

    /**
     * Creates the provider.
     *
     * @param idGenerator the generator returned by {@link #create(DeclarativeConfigProperties)}, not {@code null}
     */
    public QuarkusCdiIdGeneratorProvider(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Class<?> getType() {
        return IdGenerator.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_CDI;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return idGenerator;
    }
}
