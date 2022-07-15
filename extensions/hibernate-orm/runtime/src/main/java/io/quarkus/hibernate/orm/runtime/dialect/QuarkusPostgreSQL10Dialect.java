package io.quarkus.hibernate.orm.runtime.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;

/**
 * Subclass of PostgreSQL10Dialect fixing schema updates by considering unquoted identifiers as lower case.
 * This is PostgreSQL's behavior.
 * See https://github.com/quarkusio/quarkus/issues/1886
 */
public class QuarkusPostgreSQL10Dialect extends PostgreSQLDialect {

    public QuarkusPostgreSQL10Dialect() {
        super(DatabaseVersion.make(10, 0));
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
            throws SQLException {
        // PostgreSQL considers unquoted identifiers lowercase
        // https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
        builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.LOWER);
        // then delegate to the database metadata driver identifier casing selection
        // which can override these settings.
        return super.buildIdentifierHelper(builder, dbMetaData);
    }
}
