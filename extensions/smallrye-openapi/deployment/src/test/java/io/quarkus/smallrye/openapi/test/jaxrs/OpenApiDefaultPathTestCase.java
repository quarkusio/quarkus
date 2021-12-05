package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiDefaultPathTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.store-schema-directory=target"),
                            "application.properties"));

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
                .body("info.title", Matchers.equalTo("Generated API"))
                .body("tags.name[0]", Matchers.equalTo("test"))
                .body("paths.'/resource'.get.servers[0]", Matchers.hasKey("url"))
                .body("paths.'/resource'.get.security[0]", Matchers.hasKey("securityRequirement"))
                .body("paths.'/resource'.get", Matchers.hasKey("x-openApiExtension"));

        RestAssured.given()
                .when().options(OPEN_API_PATH)
                .then().header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
    }
}
