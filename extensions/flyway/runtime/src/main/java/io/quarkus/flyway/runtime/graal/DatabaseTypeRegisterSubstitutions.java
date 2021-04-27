package io.quarkus.flyway.runtime.graal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flywaydb.core.internal.database.DatabaseType;
import org.flywaydb.core.internal.database.DatabaseTypeRegister;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(DatabaseTypeRegister.class)
public final class DatabaseTypeRegisterSubstitutions {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static List<DatabaseType> registeredDatabaseTypes = new ArrayList<>();

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static boolean hasRegisteredDatabaseTypes = false;

    @Substitute
    private static void registerDatabaseTypes() {
        synchronized (registeredDatabaseTypes) {
            if (hasRegisteredDatabaseTypes) {
                return;
            }
            registeredDatabaseTypes.clear();

            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.cockroachdb.CockroachDBDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.redshift.RedshiftDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.mysql.mariadb.MariaDBDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.db2.DB2DatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.derby.DerbyDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.firebird.FirebirdDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.h2.H2DatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.hsqldb.HSQLDBDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.informix.InformixDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.mysql.MySQLDatabaseType());
            try {
                // Only add the OracleDatabaseType if the driver exists
                Class.forName("oracle.jdbc.OracleConnection");
                registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.oracle.OracleDatabaseType());
            } catch (ClassNotFoundException e) {
                //Ignore
            }
            try {
                // Only add the PostgreSQLDatabaseType if the driver exists
                Class.forName("org.postgresql.Driver");
                registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.postgresql.PostgreSQLDatabaseType());
            } catch (ClassNotFoundException e) {
                //Ignore
            }
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.saphana.SAPHANADatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.snowflake.SnowflakeDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.sqlite.SQLiteDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.sqlserver.SQLServerDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.sqlserver.synapse.SynapseDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.sybasease.SybaseASEJConnectDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.sybasease.SybaseASEJTDSDatabaseType());
            registeredDatabaseTypes.add(new org.flywaydb.core.internal.database.base.TestContainersDatabaseType());
            // Sort by preference order
            Collections.sort(registeredDatabaseTypes);
            hasRegisteredDatabaseTypes = true;
        }
    }
}
