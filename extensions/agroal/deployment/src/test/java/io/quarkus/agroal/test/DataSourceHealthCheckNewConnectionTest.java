package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:health-new-connection\n"
                            + "quarkus.datasource.jdbc.max-size=1\n"
                            + "quarkus.datasource.jdbc.acquisition-timeout=1S\n"
                            + "quarkus.datasource.jdbc.health-check-new-connection=true\n"
                            + "quarkus.datasource.health.enabled=true\n"), "application.properties"));

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
