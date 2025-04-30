package io.quarkus.smallrye.openapi.test.vertx;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiHttpRootDefaultPathTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo\n" +
                            "quarkus.http.cors.enabled=true\n"
                            + "quarkus.http.cors.origins=*"), "application.properties"));

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
                .body("openapi", Matchers.startsWith("3.1"))
                .body("info.title", Matchers.equalTo("quarkus-smallrye-openapi-deployment API"))
                .body("paths", Matchers.hasKey("/foo/resource"));
    }

    @Test
    public void testCorsFilterProperties() {
        // make sure CORS are present when path is not attached to main router and CORS are enabled
        RestAssured
                .given()
                .header("Origin", "https://quarkus.io")
                .get(OPEN_API_PATH)
                .then()
                .statusCode(200)
                .header("access-control-allow-origin", "https://quarkus.io")
                .header("access-control-allow-credentials", "false");
    }
}
