package io.quarkus.agroal.test;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that health check results are NOT cached when {@code quarkus.datasource.health.ttl} is {@code 0}.
 * <p>
 * After closing the datasource, a subsequent health check must report DOWN because no cached result is available.
 */
public class DataSourceHealthCheckNoTtlTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:health-no-ttl-test")
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.health.ttl", "0");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testNoCachingWithZeroTtl() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));

        dataSource.close();

        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("DOWN"));
    }
}
