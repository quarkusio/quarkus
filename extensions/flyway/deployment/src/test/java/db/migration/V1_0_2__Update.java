package db.migration;

import java.sql.Statement;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;

/**
 * Migration class for some testcases.
 */
public class V1_0_2__Update implements JavaMigration {
    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("1.0.2");
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName();
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate("INSERT INTO quarked_flyway VALUES (1002, 'test')");
        }
    }
}
