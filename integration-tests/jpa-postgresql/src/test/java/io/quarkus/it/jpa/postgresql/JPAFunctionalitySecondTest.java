package io.quarkus.it.jpa.postgresql;

import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Second test to verify container reuse behavior with PostgreSQL DevServices.
 * Verifies that containers ARE reused across test classes (marker table exists).
 * Must run after JPAFunctionalityTest.
 */
@QuarkusTest
@Order(2)
public class JPAFunctionalitySecondTest {

    @Inject
    DataSource dataSource;

    @Test
    public void base() {
        RestAssured.when().get("/jpa/testfunctionality/base").then().body(is("OK"));
    }

    @Test
    public void verifyContainerReused() throws SQLException {
        // Check if the marker table from JPAFunctionalityTest exists
        try (Connection conn = dataSource.getConnection()) {
            try {
                var rs = conn.createStatement().executeQuery("SELECT marker FROM container_reuse_marker WHERE id = 1");
                if (!rs.next()) {
                    throw new AssertionError("Marker table exists but is empty - container was reused but DB reset");
                }
                String marker = rs.getString("marker");
                if (!"test1".equals(marker)) {
                    throw new AssertionError("Expected marker 'test1' but found: " + marker);
                }
            } catch (SQLException e) {
                throw new AssertionError("Marker table does not exist - container was not reused from JPAFunctionalityTest",
                        e);
            }
        }
    }
}
