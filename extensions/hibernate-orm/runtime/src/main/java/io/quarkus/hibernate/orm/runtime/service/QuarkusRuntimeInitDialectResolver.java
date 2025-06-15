package io.quarkus.hibernate.orm.runtime.service;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * A dialect resolver used for runtime init; simply restores the dialect used during static init.
 * <p>
 * This is necessary on top of {@link QuarkusRuntimeInitDialectFactory} because schema tools, for example, bypass the
 * factory and use the dialect resolver directly.
 *
 * @see QuarkusStaticInitDialectFactory
 */
public class QuarkusRuntimeInitDialectResolver implements DialectResolver {
    private final Dialect dialect;

    public QuarkusRuntimeInitDialectResolver(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Dialect resolveDialect(DialectResolutionInfo info) {
        return dialect;
    }
}
