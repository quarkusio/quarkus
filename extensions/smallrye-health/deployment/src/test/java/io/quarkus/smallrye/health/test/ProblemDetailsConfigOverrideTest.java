package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class ProblemDetailsConfigOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailingHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.include-problem-details", "true");

    @Test
    void testProblemDetailsOverride() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health/live").then()
                    .contentType("application/problem+json")
                    .body("type", is("about:blank"),
                            "status", is(503),
                            "title", is("Health Check Failed: /q/health/live"),
                            "detail", containsString("/q/health/live, invoked at"),
                            "instance", notNullValue(),
                            "health.checks.size()", is(1),
                            "health.checks.status", contains("DOWN"),
                            "health.checks.name", contains("failing"));
        } finally {
            RestAssured.reset();
        }
    }

}
