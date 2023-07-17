package io.quarkus.smallrye.openapi.test.vertx;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiPathWithSegmentsTestCase {
    private static final String OPEN_API_PATH = "path/with/segments";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.path=" + OPEN_API_PATH),
                            "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() {
        RestAssured.given().header("Accept", "application/yaml")
                .when().get("/q/" + OPEN_API_PATH)
                .then().header("Content-Type", "application/yaml;charset=UTF-8");
        RestAssured.given().queryParam("format", "YAML")
                .when().get("/q/" + OPEN_API_PATH)
                .then().header("Content-Type", "application/yaml;charset=UTF-8");
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/" + OPEN_API_PATH)
                .then().header("Content-Type", "application/json;charset=UTF-8");
        RestAssured.given().queryParam("format", "JSON")
                .when().get("/q/" + OPEN_API_PATH)
                .then().header("Content-Type", "application/json;charset=UTF-8");
    }
}
