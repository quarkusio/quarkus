package io.quarkus.flyway.runtime.graal;

import java.sql.Connection;
import java.util.function.BooleanSupplier;

import org.flywaydb.core.api.configuration.Configuration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution replaces the call to the OracleConnection driver in Flyway when the Oracle Driver is not in the classpath.
 */
@TargetClass(className = "org.flywaydb.core.internal.database.oracle.OracleDatabaseType", onlyWith = OracleDatabaseTypeSubstitutions.IsOracleDriverAbsent.class)
public final class OracleDatabaseTypeSubstitutions {

    @Substitute
    public Connection alterConnectionAsNeeded(Connection connection, Configuration configuration) {
        return connection;
    }

    static final class IsOracleDriverAbsent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("oracle.jdbc.OracleConnection");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }

}
