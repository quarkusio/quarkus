package io.quarkus.hibernate.orm.deployment;

import java.util.List;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.deployment.spi.DatasourceDbKindHibernateOrmMetadataBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

public final class Dialects {

    private Dialects() {
        //utility
    }

    public static String guessDialect(String persistenceUnitName, String resolvedDbKind,
            List<DatasourceDbKindHibernateOrmMetadataBuildItem> dbKindMetadataBuildItems) {
        for (DatasourceDbKindHibernateOrmMetadataBuildItem item : dbKindMetadataBuildItems) {
            if (DatabaseKind.is(resolvedDbKind, item.getDbKind())) {
                return item.getDialect();
            }
        }

        String error = "The Hibernate ORM extension could not guess the dialect from the database kind '" + resolvedDbKind
                + "'. Add an explicit '" + HibernateOrmConfig.puPropertyKey(persistenceUnitName, "dialect") + "' property.";
        throw new ConfigurationException(error);
    }
}
