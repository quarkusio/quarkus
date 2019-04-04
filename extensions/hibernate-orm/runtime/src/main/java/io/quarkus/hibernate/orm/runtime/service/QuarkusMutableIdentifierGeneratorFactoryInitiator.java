package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.internal.MutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Needs to mimick MutableIdentifierGeneratorFactoryInitiator, but allows us to capture
 * which Identifier strategies are being used, so that we can keep a reference to the classed
 * needed at runtime.
 *
 * @see MutableIdentifierGeneratorFactoryInitiator
 */
public final class QuarkusMutableIdentifierGeneratorFactoryInitiator
        implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {

    private final MutableIdentifierGeneratorFactory sfScopedSingleton = new QuarkusMutableIdentifierGeneratorFactory();

    @Override
    public MutableIdentifierGeneratorFactory initiateService(final Map configurationValues,
            final ServiceRegistryImplementor registry) {
        return sfScopedSingleton;
    }

    @Override
    public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
        return MutableIdentifierGeneratorFactory.class;
    }

}
