package io.quarkus.flyway.runtime.graal;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import org.flywaydb.core.internal.parser.PeekingReader;
import org.flywaydb.core.internal.parser.Recorder;
import org.flywaydb.core.internal.parser.StatementType;
import org.flywaydb.core.internal.sqlscript.Delimiter;
import org.flywaydb.core.internal.sqlscript.ParsedSqlStatement;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution removes the PostgreSQL COPY statement support in Flyway to allow native image compilation
 * when the PostgreSQL Driver is not in the classpath.
 */
@TargetClass(className = "org.flywaydb.core.internal.database.postgresql.PostgreSQLParser", onlyWith = PostgreSQLParserSubstitutions.IsPostgreSQLDriverAbsent.class)
public final class PostgreSQLParserSubstitutions {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static StatementType COPY = new StatementType();

    /**
     * Returns only a {@link ParsedSqlStatement} as the normal SQL migrations do or throw an exception
     * if pgCopy flag is true
     */
    @Substitute
    protected ParsedSqlStatement createStatement(PeekingReader reader, Recorder recorder,
            int statementPos, int statementLine, int statementCol,
            int nonCommentPartPos, int nonCommentPartLine, int nonCommentPartCol,
            StatementType statementType, boolean canExecuteInTransaction,
            Delimiter delimiter, String sql) throws IOException {
        if (statementType == COPY) {
            // this is the only modification this substitution do
            throw new IllegalStateException("pgCopy is not supported yet!");
        }

        return new ParsedSqlStatement(statementPos, statementLine, statementCol,
                sql, delimiter, canExecuteInTransaction);
    }

    static final class IsPostgreSQLDriverAbsent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.postgresql.Driver");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
