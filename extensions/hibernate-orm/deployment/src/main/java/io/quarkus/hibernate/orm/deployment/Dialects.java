package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.configuration.ConfigurationError;

public final class Dialects {

    private Dialects() {
        //utility
    }

    public static String guessDialect(String resolvedDbKind) {
        // For now select the latest dialect from the driver
        // later, we can keep doing that but also avoid DCE
        // of all the dialects we want in so that people can override them
        if (DatabaseKind.isDB2(resolvedDbKind)) {
            return "org.hibernate.dialect.DB297Dialect";
        }
        if (DatabaseKind.isPostgreSQL(resolvedDbKind)) {
            return "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect";
        }
        if (DatabaseKind.isH2(resolvedDbKind)) {
            return "io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect";
        }
        if (DatabaseKind.isMariaDB(resolvedDbKind)) {
            return "org.hibernate.dialect.MariaDB103Dialect";
        }
        if (DatabaseKind.isMySQL(resolvedDbKind)) {
            return "org.hibernate.dialect.MySQL8Dialect";
        }
        if (DatabaseKind.isOracle(resolvedDbKind)) {
            return "org.hibernate.dialect.Oracle12cDialect";
        }
        if (DatabaseKind.isDerby(resolvedDbKind)) {
            return "org.hibernate.dialect.DerbyTenSevenDialect";
        }
        if (DatabaseKind.isMsSQL(resolvedDbKind)) {
            return "org.hibernate.dialect.SQLServer2012Dialect";
        }

        String error = "Hibernate extension could not guess the dialect from the database kind '" + resolvedDbKind
                + "'. Add an explicit '" + HibernateOrmProcessor.HIBERNATE_ORM_CONFIG_PREFIX + "dialect' property.";
        throw new ConfigurationError(error);
    }
}
