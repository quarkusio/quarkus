package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class MaxHealthGroupTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.max-group-registries-count", "3");

    @Test
    void testMaxGroupRegistriesCreations() {
        try {
            RestAssured.defaultParser = Parser.JSON;

            for (int i = 0; i < 3; i++) {
                RestAssured.get("/q/health/group/" + i).then()
                        .statusCode(200)
                        .body("status", is("UP"),
                                "checks.size()", is(0));
            }
            RestAssured.when().get("/q/health/group/not-allowed").then()
                    .statusCode(500)
                    .body("details", Matchers.endsWith("3"));
        } finally {
            RestAssured.reset();
        }
    }

}
