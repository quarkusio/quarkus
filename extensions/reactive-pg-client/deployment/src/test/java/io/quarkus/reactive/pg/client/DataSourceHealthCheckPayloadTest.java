package io.quarkus.reactive.pg.client;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DataSourceHealthCheckPayloadTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void testDataSourceHealthCheckPayload() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("DOWN"))
                .body("checks.data", CoreMatchers
                        .hasItem(Matchers.hasValue(CoreMatchers.containsString("down - connection failed"))));
    }

}
