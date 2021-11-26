package io.quarkus.flyway.runtime.graal;

import java.util.function.BooleanSupplier;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.util.ClassUtils;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.flywaydb.database.sqlserver.SQLServerDatabaseType", onlyWith = SQLServerDatabaseTypeSubstitutions.SQLServerAvailable.class)
public final class SQLServerDatabaseTypeSubstitutions {

    @Substitute
    public Object createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor) {
        return new SQLServerDatabaseSubstitution(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @TargetClass(className = "org.flywaydb.database.sqlserver.SQLServerDatabase", onlyWith = SQLServerAvailable.class)
    public static final class SQLServerDatabaseSubstitution {

        @Alias
        public SQLServerDatabaseSubstitution(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                StatementInterceptor statementInterceptor) {
        }
    }

    public static final class SQLServerAvailable implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ClassUtils.isPresent("org.flywaydb.database.sqlserver.SQLServerDatabaseType",
                    Thread.currentThread().getContextClassLoader());
        }
    }

}
