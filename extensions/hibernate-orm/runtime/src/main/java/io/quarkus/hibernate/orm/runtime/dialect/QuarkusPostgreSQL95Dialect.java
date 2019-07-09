package io.quarkus.hibernate.orm.runtime.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;

/**
 * Subclass of PostgreSQL95Dialect fixing schema updates by considering unquoted identifiers as lower case.
 * This is PostgreSQL's behavior.
 * See https://github.com/quarkusio/quarkus/issues/1886
 *
 * @author Stephane Epardaud
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class QuarkusPostgreSQL95Dialect extends PostgreSQL95Dialect {
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
