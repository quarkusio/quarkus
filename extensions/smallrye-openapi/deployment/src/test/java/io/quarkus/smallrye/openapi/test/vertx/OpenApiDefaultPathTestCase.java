package io.quarkus.smallrye.openapi.test.vertx;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiDefaultPathTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class));

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
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.startsWith("3.0"))
                .body("info.title", Matchers.equalTo("quarkus-smallrye-openapi-deployment API"))
                .body("paths", Matchers.hasKey("/resource"));
    }

    @Test
    public void testDefaultOpenApiCorsProperties() {
        // make sure default CORS properties are present
        RestAssured
                .given()
                .header("Origin", "https://quarkus.io")
                .get(OPEN_API_PATH)
                .then()
                .statusCode(200)
                .header("access-control-allow-methods", "GET, HEAD, OPTIONS")
                .header("access-control-allow-headers", "Content-Type, Authorization")
                .header("access-control-max-age", "86400")
                .header("access-control-allow-origin", "*")
                .header("access-control-allow-credentials", "true");
    }
}
