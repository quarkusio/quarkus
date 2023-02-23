package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Objects;

import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.dialect.Dialect;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;

/**
 * Customizes the DefaultDialectSelector to ensure we use the custom Quarkus dialect for H2;
 * this is necessary as we need to strictly match the H2 version bundled with Quarkus:
 * normally Hibernate ORM would match this from connection metadata, but we don't have that metadata.
 */
final class QuarkusDialectSelector extends DefaultDialectSelector {

    @Override
    public Class<? extends Dialect> resolve(final String name) {
        Objects.requireNonNull(name);
        if (name.isEmpty()) {
            return null;
        }
        switch (name) {
            case "org.hibernate.dialect.H2Dialect":
                Logger.getLogger(QuarkusDialectSelector.class)
                        .warn("Overriding dialect choice: 'org.hibernate.dialect.H2Dialect' will be replaced with '"
                                + QuarkusH2Dialect.class.getName()
                                + "' to ensure compatibility with the bundled version of H2.");
            case "H2":
                return QuarkusH2Dialect.class;
            default:
                return super.resolve(name);
        }
    }

}
