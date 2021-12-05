package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.h2.jdbc.JdbcSQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionCleanAtStartTest {

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("clean-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Clean at start correctly")
    public void testFlywayConfigInjection() throws SQLException {

        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select * from fake_existing_tbl")) {
                fail("fake_existing_tbl should not exist");
            } catch (JdbcSQLException e) {
                // expected fake_existing_tbl does not exist
            }
        }
        MigrationInfo current = flyway.info().current();
        assertNull(current, "Info is not null");
    }
}
