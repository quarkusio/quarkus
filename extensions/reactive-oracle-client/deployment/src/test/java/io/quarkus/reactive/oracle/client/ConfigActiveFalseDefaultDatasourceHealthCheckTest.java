package io.quarkus.reactive.oracle.client;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConfigActiveFalseDefaultDatasourceHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            .overrideConfigKey("quarkus.datasource.active", "false")
            // this data source is broken, but will be deactivated,
            // so the overall check should pass
            .overrideConfigKey("quarkus.datasource.reactive.url", "BROKEN");

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"))
                // If the datasource is inactive, there should not be a health check
                .body("checks[0].data.\"<default>\"", CoreMatchers.nullValue());
    }

}
