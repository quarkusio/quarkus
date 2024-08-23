package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.health.test.ui.HealthGroupCheck;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class DefaultHealthGroupTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class, HealthGroupCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.default-health-group", "my-default-health-group");

    @Test
    void testDefaultHealthGroup() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health/group/my-default-health-group").then()
                    .body("status", is("UP"),
                            "checks.size()", is(1),
                            "checks.status", contains("UP"),
                            "checks.name", contains("basic"));

            RestAssured.when().get("/q/health/group/test-group").then()
                    .body("status", is("UP"),
                            "checks.size()", is(1),
                            "checks.status", contains("UP"),
                            "checks.name", contains(HealthGroupCheck.class.getName()));

            RestAssured.when().get("/q/health/group").then()
                    .body("status", is("UP"),
                            "checks.size()", is(2),
                            "checks.status", hasItems("UP", "UP"),
                            "checks.name", hasItems("basic", HealthGroupCheck.class.getName()));
        } finally {
            RestAssured.reset();
        }
    }

}
