package io.quarkus.agroal.test;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConfigActiveFalseNamedDatasourceHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            .overrideConfigKey("quarkus.datasource.users.active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2")
            // this data source is broken, but will be deactivated,
            // so the overall check should pass
            .overrideConfigKey("quarkus.datasource.users.jdbc.url", "BROKEN");

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"))
                // If the datasource is inactive, there should not be a health check
                .body("checks[0].data.\"users\"", CoreMatchers.nullValue());
    }

}
