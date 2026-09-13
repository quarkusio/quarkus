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
 * By default, the readiness check only validates a pooled connection, so a pool whose connections are all in use
 * reports the datasource as DOWN.
 */
public class DataSourceHealthCheckExhaustedPoolTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:health-exhausted-pool\n"
                            + "quarkus.datasource.jdbc.max-size=1\n"
                            + "quarkus.datasource.jdbc.acquisition-timeout=1S\n"
                            + "quarkus.datasource.health.enabled=true\n"), "application.properties"));

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testExhaustedPoolIsDown() throws SQLException {
        try (Connection leased = dataSource.getConnection()) {
            RestAssured.when().get("/q/health/ready")
                    .then()
                    .body("status", CoreMatchers.equalTo("DOWN"));
        }
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));
    }

}
