package io.quarkus.agroal.test;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DataSourceHealthCheckConfigActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.datasource.health.enabled", "true")
            // this data source is broken, but will be deactivated,
            // so the overall check should pass
            .overrideConfigKey("quarkus.datasource.brokenDS.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.brokenDS.jdbc.url", "BROKEN")
            .overrideConfigKey("quarkus.datasource.brokenDS.active", "false");

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));
    }

}
