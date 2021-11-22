package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.sqlserver.SQLServerDatabase;
import org.flywaydb.core.internal.database.sqlserver.SQLServerDatabaseType;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(SQLServerDatabaseType.class)
public final class SQLServerDatabaseTypeSubstitutions {
    @Substitute
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor) {
        return new SQLServerDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }
}
