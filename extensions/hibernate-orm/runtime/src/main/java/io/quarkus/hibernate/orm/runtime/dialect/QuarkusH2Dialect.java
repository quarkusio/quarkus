package io.quarkus.hibernate.orm.runtime.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;

/**
 * Subclass of H2Dialect fixing schema updates by considering unquoted identifiers as upper case.
 * This is H2's default.
 * See https://github.com/quarkusio/quarkus/issues/1886
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class QuarkusH2Dialect extends H2Dialect {
    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
            throws SQLException {
        // H2 by default consider identifiers as upper case
        // unless DATABASE_TO_UPPER=false but that's not the default
        // and Thomas Mueller says it's normal ANSI-SQL SQL-92 behavior
        // http://h2-database.66688.n3.nabble.com/Case-of-column-name-td3485519.html
        // http://www.h2database.com/html/grammar.html#name
        // DATABASE_TO_LOWER=TRUE will come with H2's next version as of Feb 2019 but only for PostgreSQL compat
        builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.UPPER);
        // then delegate to the database metadata driver identifier casing selection
        // which can override these settings.
        return super.buildIdentifierHelper(builder, dbMetaData);
    }
}
