package io.quarkus.agroal.test;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConfigUrlMissingDefaultDatasourceHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                // UnconfiguredDataSource always reports as healthy...
                // https://github.com/quarkusio/quarkus/issues/36666
                .body("status", CoreMatchers.equalTo("UP"))
                .body("checks[0].data.\"<default>\"", CoreMatchers.equalTo("UP"));
    }

}
