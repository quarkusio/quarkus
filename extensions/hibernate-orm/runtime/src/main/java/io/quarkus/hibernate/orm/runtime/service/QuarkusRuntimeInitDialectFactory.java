package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;

/**
 * A dialect factory used for runtime init;
 * simply restores the dialect used during static init.
 *
 * @see QuarkusStaticInitDialectFactory
 */
public class QuarkusRuntimeInitDialectFactory implements DialectFactory {
    private final Dialect dialect;

    public QuarkusRuntimeInitDialectFactory(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Dialect buildDialect(Map<String, Object> configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        return dialect;
    }

}
