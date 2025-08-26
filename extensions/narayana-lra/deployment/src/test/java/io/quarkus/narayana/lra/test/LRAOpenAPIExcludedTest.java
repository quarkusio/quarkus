package io.quarkus.narayana.lra.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LRAOpenAPIExcludedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.lra.openapi.included", "false")
            .overrideConfigKey("quarkus.lra.devservices.enabled", "false");

    @Test
    public void testLRAExcluded() {
        RestAssured.when().get("/q/openapi").then()
                .body(not(containsString("lraproxy")), not(containsString("lra-participant-proxy")),
                        not(containsString("LRAStatus")));
    }
}
