package io.quarkus.smallrye.metrics.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verify that the metrics handler works on the expected path
 * when quarkus.http.root-path and quarkus.http.non-application-root-path are set.
 */
public class MetricsHandlerPathTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.http.root-path", "/custom")
            .overrideConfigKey("quarkus.http.non-application-root-path", "/framework");

    @Test
    public void testMetricsEndpointAccessibility() {
        RestAssured.basePath = "/";
        RestAssured.when()
                // no need to prepend the /custom here because it will be reflected in RestAssured.basePath
                .get("/framework/metrics")
                .then()
                .statusCode(200);
    }

}
