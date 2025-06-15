package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Copied from org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator
 */
public class QuarkusStaticInitDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

    public static final QuarkusStaticInitDialectFactoryInitiator INSTANCE = new QuarkusStaticInitDialectFactoryInitiator();

    @Override
    public Class<DialectFactory> getServiceInitiated() {
        return DialectFactory.class;
    }

    @Override
    public DialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusStaticInitDialectFactory();
    }
}
