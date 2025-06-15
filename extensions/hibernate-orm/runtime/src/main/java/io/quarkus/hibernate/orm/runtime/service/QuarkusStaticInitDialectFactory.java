package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.service.spi.ServiceRegistryAwareService;

/**
 * A dialect factory used for static init; the same as Hibernate ORM's default one except it records the dialect so that
 * we can reuse it at runtime init.
 */
public class QuarkusStaticInitDialectFactory extends DialectFactoryImpl
        implements DialectFactory, ServiceRegistryAwareService {

    private Dialect dialect;

    @Override
    public Dialect buildDialect(Map<String, Object> configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        dialect = super.buildDialect(configValues, resolutionInfoSource);
        return dialect;
    }

    public Dialect getDialect() {
        return dialect;
    }

}
