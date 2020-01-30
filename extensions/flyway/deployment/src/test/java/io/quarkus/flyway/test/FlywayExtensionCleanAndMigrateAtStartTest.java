package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionCleanAndMigrateAtStartTest {

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("clean-and-migrate-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Clean and migrate at start correctly")
    public void testFlywayConfigInjection() throws SQLException {

        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select * from fake_existing_tbl")) {
                assertFalse(executeQuery.next(), "Table exists but is not empty");
            }
        }
        String currentVersion = flyway.info().current().getVersion().toString();
        assertEquals("1.0.0", currentVersion, "Expected to be 1.0.0 as migration runs at start");
    }
}
