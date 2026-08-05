package io.quarkus.narayana.lra.test;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class LRAOpenAPIIncludedTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.lra.openapi.included", "true")
            .overrideConfigKey("quarkus.lra.devservices.enabled", "false");

    @Test
    public void testLRAIncluded() {
        RestAssured.when().get("/q/openapi").then()
                .body(containsString("lraproxy"), containsString("lra-participant-proxy"), containsString("LRAStatus"));
    }
}
