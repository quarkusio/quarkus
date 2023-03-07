package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusRuntimeInitDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

    private final Dialect dialect;

    public QuarkusRuntimeInitDialectFactoryInitiator(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Class<DialectFactory> getServiceInitiated() {
        return DialectFactory.class;
    }

    @Override
    public DialectFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusRuntimeInitDialectFactory(dialect);
    }
}
