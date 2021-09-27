package io.quarkus.it.spring.web.openapi;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public class OpenApiPathWithoutSegmentsPMT {
    private static final String OPEN_API_PATH = "path-without-segments";

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(OpenApiController.class)
                    .addAsResource("test-roles.properties")
                    .addAsResource("test-users.properties"))
            .overrideConfigKey("quarkus.smallrye-openapi.path", OPEN_API_PATH)
            .setRun(true);

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
