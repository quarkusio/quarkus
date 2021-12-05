package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionCallbackTest {

    // Quarkus built object
    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    static final FlywayH2TestCustomizer customizer = FlywayH2TestCustomizer
            .withDbName("quarkus-flyway-callback")
            .withPort(11303)
            .withInitSqlFile("src/test/resources/callback-init-data.sql");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setBeforeAllCustomizer(customizer::startH2)
            .setAfterAllCustomizer(customizer::stopH2)
            .withApplicationRoot((jar) -> jar
                    .addClasses(FlywayH2TestCustomizer.class,
                            FlywayExtensionCallback.class, FlywayExtensionCallback2.class)
                    .addAsResource("db/migration/V1.0.3__Quarkus_Callback.sql")
                    .addAsResource("callback-config.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start correctly and executes callback")
    public void testFlywayCallback() throws SQLException {
        MigrationInfo migrationInfo = flyway.info().current();
        assertNotNull(migrationInfo, "No Flyway migration was executed");

        String currentVersion = migrationInfo.getVersion().toString();
        // Expected to be 1.0.3 as migration runs at start
        assertEquals("1.0.3", currentVersion);
        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select COUNT(name) from quarked_callback")) {
                assertTrue(executeQuery.next(), "Table exists but it is empty");
                int count = executeQuery.getInt(1);
                // Expect one row for each callback invoked by Flyway
                int expected = FlywayExtensionCallback.DEFAULT_EVENTS.size() + FlywayExtensionCallback2.DEFAULT_EVENTS.size();
                assertEquals(expected, count);
            }
        }
    }
}
