package io.quarkus.reactive.oracle.client;

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
                // When the URL is missing, the client assumes a default one.
                // See https://github.com/quarkusio/quarkus/issues/43517
                // In this case the default won't work, resulting in a failing health check.
                .body("status", CoreMatchers.equalTo("DOWN"))
                .body("checks[0].data.\"<default>\"", CoreMatchers.startsWithIgnoringCase("DOWN"));
    }

}
