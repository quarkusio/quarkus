package io.quarkus.it.jpa.postgresql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Creates a marker table to verify container reuse in JPAFunctionalitySecondTest.
 */
@QuarkusTest
@Order(1)
public class JPAFunctionalityContainerReuseTest {

    @Inject
    DataSource dataSource;

    @Test
    public void createMarkerForContainerReuseTest() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS container_reuse_marker (id INT PRIMARY KEY, marker TEXT)");
            conn.createStatement().execute(
                    "INSERT INTO container_reuse_marker (id, marker) VALUES (1, 'test1') ON CONFLICT DO NOTHING");
        }
    }
}
