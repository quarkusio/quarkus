package io.quarkus.hibernate.reactive.deployment;

import java.util.List;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * This used to be the approach before 6bf38240 in the Hibernate ORM extension as well.
 * Align to ORM? TBD
 */
@Deprecated
final class Dialects {

    private Dialects() {
        //utility
    }

    public static String guessDialect(String persistenceUnitName, String resolvedDbKind,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        for (DatabaseKindDialectBuildItem item : dbKindDialectBuildItems) {
            if (DatabaseKind.is(resolvedDbKind, item.getDbKind())) {
                return item.getDialect();
            }
        }

        String error = "The Hibernate ORM extension could not guess the dialect from the database kind '" + resolvedDbKind
                + "'. Add an explicit '" + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect")
                + "' property.";
        throw new ConfigurationException(error);
    }
}
