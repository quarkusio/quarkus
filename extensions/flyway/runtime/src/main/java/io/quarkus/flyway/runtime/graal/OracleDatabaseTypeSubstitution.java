package io.quarkus.flyway.runtime.graal;

import java.sql.Connection;
import java.util.function.BooleanSupplier;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.util.ClassUtils;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Avoid loading the oracle.jdbc.OracleConnection class if unavailable
 */
@TargetClass(className = "org.flywaydb.database.oracle.OracleDatabaseType", onlyWith = {
        OracleDatabaseTypeSubstitution.OracleAvailable.class,
        OracleDatabaseTypeSubstitution.OracleDriverUnavailable.class })
public final class OracleDatabaseTypeSubstitution {

    @Substitute
    public Connection alterConnectionAsNeeded(Connection connection, Configuration configuration) {
        return connection;
    }

    public static final class OracleDriverUnavailable implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !ClassUtils.isPresent("oracle.jdbc.OracleConnection",
                    Thread.currentThread().getContextClassLoader());
        }
    }

    public static final class OracleAvailable implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ClassUtils.isPresent("org.flywaydb.database.oracle.OracleDatabaseType",
                    Thread.currentThread().getContextClassLoader());
        }
    }
}
