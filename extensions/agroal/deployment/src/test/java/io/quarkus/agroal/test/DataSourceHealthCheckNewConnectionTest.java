package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * With {@code quarkus.datasource.jdbc.health-check-new-connection=true}, the readiness check validates the datasource
 * through a new connection when all pooled connections are in use.
 */
public class DataSourceHealthCheckNewConnectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:health-new-connection")
            .overrideConfigKey("quarkus.datasource.jdbc.max-size", "1")
            .overrideConfigKey("quarkus.datasource.jdbc.acquisition-timeout", "1S")
            .overrideConfigKey("quarkus.datasource.jdbc.health-check-new-connection", "true")
            .overrideConfigKey("quarkus.datasource.health.enabled", "true");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testExhaustedPoolIsUp() throws SQLException {
        try (Connection leased = dataSource.getConnection()) {
            RestAssured.when().get("/q/health/ready")
                    .then()
                    .body("status", CoreMatchers.equalTo("UP"));
        }
    }

}
