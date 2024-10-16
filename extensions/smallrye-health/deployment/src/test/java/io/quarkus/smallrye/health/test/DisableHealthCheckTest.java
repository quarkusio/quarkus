package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class DisableHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.check.\""
                    + BasicHealthCheck.class.getName() + "\".enabled", "false");

    @Test
    void testHealthCheckDisabled() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health").then()
                    .body("status", is("UP"),
                            "checks.size()", is(0));
        } finally {
            RestAssured.reset();
        }
    }

}
