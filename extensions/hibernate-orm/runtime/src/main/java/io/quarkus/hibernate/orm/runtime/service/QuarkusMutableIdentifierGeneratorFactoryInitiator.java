package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * We need to mimic the standard IdentifierGeneratorFactory but allowing
 * to capture which Identifier strategies are being used, so that we can keep a reference to the classed
 * needed at runtime.
 *
 * @see IdentifierGeneratorFactory
 */
public final class QuarkusMutableIdentifierGeneratorFactoryInitiator
        implements StandardServiceInitiator<IdentifierGeneratorFactory> {

    @Override
    public IdentifierGeneratorFactory initiateService(final Map configurationValues,
            final ServiceRegistryImplementor registry) {
        return new QuarkusMutableIdentifierGeneratorFactory(registry);
    }

    @Override
    public Class<IdentifierGeneratorFactory> getServiceInitiated() {
        return IdentifierGeneratorFactory.class;
    }

}
