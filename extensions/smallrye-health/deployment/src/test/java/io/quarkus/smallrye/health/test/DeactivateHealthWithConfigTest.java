package io.quarkus.smallrye.health.test;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class DeactivateHealthWithConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-health.enabled", "false");

    @Test
    void testAdditionalJsonPropertyInclusions() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health").then()
                    .statusCode(404);
        } finally {
            RestAssured.reset();
        }
    }

}
