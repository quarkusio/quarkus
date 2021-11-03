package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.h2.jdbc.JdbcSQLException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionFilesystemResourceTest {

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(V1_0_1__Update.class, V1_0_2__Update.class)
                    .addAsResource("clean-and-migrate-at-start-with-fs-resource-config.properties", "application.properties"));

    @Test
    @DisplayName("Clean and migrate at start correctly")
    public void testFlywayConfigInjection() throws SQLException {

        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select * from fake_existing_tbl")) {
                fail("fake_existing_tbl should not exist. Clean was run at start");
            } catch (JdbcSQLException e) {
                // expected fake_existing_tbl does not exist
            }
            try (ResultSet countQuery = stat.executeQuery("select count(1) from quarked_flyway")) {
                assertTrue(countQuery.first());
                assertEquals(2,
                        countQuery.getInt(1),
                        "Table 'quarked_flyway' does not contain the expected number of rows");
            }
        }
        String currentVersion = flyway.info().current().getVersion().toString();
        assertEquals("1.0.3", currentVersion, "Expected to be 1.0.3 as there is a SQL and two Java migration scripts");
    }

    public static class V1_0_1__Update extends BaseJavaMigration {
        @Override
        public void migrate(Context context) throws Exception {
            try (Statement statement = context.getConnection().createStatement()) {
                statement.executeUpdate("INSERT INTO quarked_flyway VALUES (1001, 'test')");
            }
        }
    }

    public static class V1_0_2__Update implements JavaMigration {
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
        public boolean isUndo() {
            return false;
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

        @Override
        public boolean isBaselineMigration() {
            return false;
        }
    }

}
