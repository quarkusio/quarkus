package io.quarkus.agroal.test;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DataSourceHealthCheckExclusionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .withConfigurationResource("application-datasources-with-health.properties");

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));
    }

}
