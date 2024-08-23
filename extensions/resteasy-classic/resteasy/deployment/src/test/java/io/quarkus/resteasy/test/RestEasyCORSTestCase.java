package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestEasyCORSTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootResource.class)
                    .addAsResource("cors-config.properties", "application.properties"));

    @Test
    public void testCORSPreflightRootResource() {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET,POST";
        String headers = "X-Custom";
        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when().options("/").then()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers);
    }

    @Test
    public void testCORSRootResource() {
        String origin = "http://custom.origin.quarkus";
        RestAssured.given()
                .header("Origin", origin)
                .when().get("/").then()
                .header("Access-Control-Allow-Origin", origin)
                .body(Matchers.is("Root Resource"));
    }
}
