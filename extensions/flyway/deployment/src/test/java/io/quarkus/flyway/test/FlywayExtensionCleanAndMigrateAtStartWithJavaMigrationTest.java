package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionCleanAndMigrateAtStartWithJavaMigrationTest {

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(V1_0_1__Update.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("clean-and-migrate-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Clean and migrate at start correctly")
    public void testFlywayConfigInjection() throws SQLException {

        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet countQuery = stat.executeQuery("select count(1) from quarked_flyway")) {
                countQuery.first();
                int count = countQuery.getInt(1);
                assertEquals(1, count, "Table 'quarked_flyway' does not contain the expected number of rows");
            }
        }
        String currentVersion = flyway.info().current().getVersion().toString();
        assertEquals("1.0.1", currentVersion, "Expected to be 1.0.1 as there is both a SQL and a Java migration script");
    }

    public static class V1_0_1__Update extends BaseJavaMigration {
        @Override
        public void migrate(Context context) throws Exception {
            try (Statement statement = context.getConnection().createStatement()) {
                statement.executeUpdate("INSERT INTO quarked_flyway VALUES (1001, 'test')");
            }
        }
    }
}
