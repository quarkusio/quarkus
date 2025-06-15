package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusRuntimeInitDialectResolverInitiator implements StandardServiceInitiator<DialectResolver> {

    private final Dialect dialect;

    public QuarkusRuntimeInitDialectResolverInitiator(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Class<DialectResolver> getServiceInitiated() {
        return DialectResolver.class;
    }

    @Override
    public DialectResolver initiateService(Map<String, Object> configurationValues,
            ServiceRegistryImplementor registry) {
        return new QuarkusRuntimeInitDialectResolver(dialect);
    }
}
