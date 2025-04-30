package io.quarkus.narayana.lra.test;

import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LRAOpenAPIIncludedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(new StringAsset("quarkus.lra.openapi.included=true"),
                    "application.properties"));

    @Test
    public void testLRAIncluded() {
        RestAssured.when().get("/q/openapi").then()
                .body(containsString("lraproxy"), containsString("lra-participant-proxy"), containsString("LRAStatus"));
    }
}
