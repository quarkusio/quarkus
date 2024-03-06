package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class AdditionalJsonPropertiesConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.additional.property.testProp1", "testValue1")
            .overrideConfigKey("quarkus.smallrye-health.additional.property.testProp2", "testValue2");

    @Test
    void testAdditionalJsonPropertyInclusions() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health").then()
                    .body("status", is("UP"),
                            "checks.size()", is(1),
                            "checks.status", contains("UP"),
                            "checks.name", contains("basic"),
                            "testProp1", is("testValue1"),
                            "testProp2", is("testValue2"));
        } finally {
            RestAssured.reset();
        }
    }

}
