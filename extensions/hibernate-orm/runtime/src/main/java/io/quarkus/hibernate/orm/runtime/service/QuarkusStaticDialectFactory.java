package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;

public class QuarkusStaticDialectFactory implements DialectFactory {
    private final Dialect dialect;

    public QuarkusStaticDialectFactory(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
        return dialect;
    }
}
