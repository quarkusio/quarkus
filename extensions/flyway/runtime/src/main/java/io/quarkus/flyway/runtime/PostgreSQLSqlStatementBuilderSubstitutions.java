package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.internal.line.Line;
import org.flywaydb.core.internal.sqlscript.Delimiter;
import org.flywaydb.core.internal.sqlscript.SqlStatement;
import org.flywaydb.core.internal.sqlscript.StandardSqlStatement;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author cristhiank on 2019-03-19
 **/
@TargetClass(className = "org.flywaydb.core.internal.database.postgresql.PostgreSQLSqlStatementBuilder")
public final class PostgreSQLSqlStatementBuilderSubstitutions {
    @Alias
    private boolean pgCopy;
    @Alias
    protected List<Line> lines = new ArrayList<>();
    @Alias
    protected Delimiter delimiter;

    @Substitute
    public SqlStatement getSqlStatement() {
        if (pgCopy) {
            throw new IllegalStateException("pgCopy is not supported yet!");
        }
        return new StandardSqlStatement(lines, delimiter);
    }
}
