package io.quarkus.it.spring.web.openapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public class OpenApiPathWithSegmentsPMT {
    private static final String OPEN_API_PATH = "/path/with/segments";

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiController.class)
                    .addAsResource("test-roles.properties")
                    .addAsResource("test-users.properties"))
            .overrideConfigKey("quarkus.smallrye-openapi.path", OPEN_API_PATH)
            .setRun(true);

    @Test
    public void testOpenApiPathAccessResource() {
        RestAssured.given().header("Accept", "application/yaml")
                .when().get(OPEN_API_PATH)
                .then().header("Content-Type", "application/yaml;charset=UTF-8");
        RestAssured.given().queryParam("format", "YAML")
                .when().get(OPEN_API_PATH)
                .then().header("Content-Type", "application/yaml;charset=UTF-8");
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then().header("Content-Type", "application/json;charset=UTF-8");
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then().header("Content-Type", "application/json;charset=UTF-8");
    }
}
