package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.reactive.dialect.ReactiveDialectWrapper;
import org.hibernate.service.spi.ServiceRegistryAwareService;

import io.quarkus.hibernate.orm.runtime.service.QuarkusStaticInitDialectFactory;

public class ReactiveQuarkusStaticInitDialectFactory extends QuarkusStaticInitDialectFactory
        implements DialectFactory, ServiceRegistryAwareService {

    private Dialect wrapDialect;

    @Override
    public Dialect buildDialect(Map<String, Object> map, DialectResolutionInfoSource dialectResolutionInfoSource)
            throws HibernateException {
        this.wrapDialect = new ReactiveDialectWrapper(super.buildDialect(map, dialectResolutionInfoSource));
        return this.wrapDialect;
    }

    @Override
    public Dialect getDialect() {
        return this.wrapDialect;
    }
}
