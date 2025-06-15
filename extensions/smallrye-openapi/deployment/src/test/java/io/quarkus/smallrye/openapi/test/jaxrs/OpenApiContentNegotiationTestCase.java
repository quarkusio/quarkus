package io.quarkus.smallrye.openapi.test.jaxrs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiContentNegotiationTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(OpenApiResource.class, ResourceBean.class));

    @Test
    public void testHeaderJson() {
        RestAssured.given().header("Accept", "application/json").when().get(OPEN_API_PATH).then().header("Content-Type",
                "application/json;charset=UTF-8");
    }

    @Test
    public void testHeaderYaml() {
        RestAssured.given().header("Accept", "application/yaml").when().get(OPEN_API_PATH).then().header("Content-Type",
                "application/yaml;charset=UTF-8");
    }

    @Test
    public void testFormatJson() {
        RestAssured.given().when().get(OPEN_API_PATH + "?format=json").then().header("Content-Type",
                "application/json;charset=UTF-8");
    }

    @Test
    public void testFormatYaml() {
        RestAssured.given().when().get(OPEN_API_PATH + "?format=yaml").then().header("Content-Type",
                "application/yaml;charset=UTF-8");
    }

    @Test
    public void testExtensionJson() {
        RestAssured.given().when().get(OPEN_API_PATH + ".json").then().header("Content-Type",
                "application/json;charset=UTF-8");
    }

    @Test
    public void testExtensionYaml() {
        RestAssured.given().when().get(OPEN_API_PATH + ".yaml").then().header("Content-Type",
                "application/yaml;charset=UTF-8");
    }

    @Test
    public void testDefault() {
        RestAssured.given().when().get(OPEN_API_PATH).then().header("Content-Type", "application/yaml;charset=UTF-8");
    }
}
