package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.internal.StandardIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Uses a StandardIdentifierGeneratorFactory, but one that doesn't retrieve generators from CDI.
 *
 * @see IdentifierGeneratorFactory
 */
public final class QuarkusIdentifierGeneratorFactoryInitiator
        implements StandardServiceInitiator<IdentifierGeneratorFactory> {

    @Override
    public IdentifierGeneratorFactory initiateService(final Map configurationValues,
            final ServiceRegistryImplementor registry) {
        return new StandardIdentifierGeneratorFactory(registry, true /* ignore bean container */);
    }

    @Override
    public Class<IdentifierGeneratorFactory> getServiceInitiated() {
        return IdentifierGeneratorFactory.class;
    }

}
