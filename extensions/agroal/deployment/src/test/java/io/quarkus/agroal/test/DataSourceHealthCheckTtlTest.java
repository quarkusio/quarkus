package io.quarkus.agroal.test;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that health check results are cached when {@code quarkus.datasource.health.ttl} is set.
 * <p>
 * The test performs a health check (UP), then closes the datasource so that subsequent real checks
 * would fail. A second call within the TTL window must still return UP from the cache.
 */
public class DataSourceHealthCheckTtlTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:health-ttl-test")
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.health.ttl", "10m");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testCachedResultReturnedWithinTtl() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"))
                .body("checks[0].data.\"<default>\"", CoreMatchers.equalTo("UP"));

        dataSource.close();

        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"))
                .body("checks[0].data.\"<default>\"", CoreMatchers.equalTo("UP"));
    }
}
