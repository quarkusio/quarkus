package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AutoAddOpenApiEndpointFilterTest {
    private static final String OPEN_API_PATH = "/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-openapi.auto-add-open-api-endpoint=true
                                    quarkus.smallrye-openapi.path=%s
                                    """.formatted(OPEN_API_PATH)),

                            "application.properties"));

    @Test
    public void testOpenApiFilterResource() {
        var endpointPath = "paths.'%s'.get".formatted(OPEN_API_PATH);
        var contentPath = "paths.'%s'.get.responses.200.content".formatted(OPEN_API_PATH);
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey(OPEN_API_PATH))
                .body(endpointPath + ".tags", Matchers.hasItem("openapi"))
                .body(endpointPath + ".responses", Matchers.hasKey("200"))
                .body(endpointPath + ".operationId", Matchers.equalTo("getOpenAPISpecification"))
                .body(contentPath, Matchers.hasKey("application/json"))
                .body(contentPath, Matchers.hasKey("application/yaml"))
                .body(contentPath + ".size()", Matchers.equalTo(2))
                .body("tags", Matchers.hasItem(Matchers.hasEntry("name", "openapi")));
    }

    @Test
    public void testSpecificYamlEndpointFilterResource() {
        var yamlPath = OPEN_API_PATH + ".yaml";
        var endpointPath = "paths.'%s'.get".formatted(yamlPath);
        var contentPath = "paths.'%s'.get.responses.200.content".formatted(yamlPath);
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey(yamlPath))
                .body(endpointPath + ".operationId", Matchers.equalTo("getOpenAPISpecificationYaml"))
                .body(contentPath, Matchers.hasKey("application/yaml"))
                .body(contentPath + ".size()", Matchers.equalTo(1));
    }

    @Test
    public void testSpecificYmlEndpointFilterResource() {
        var ymlPath = OPEN_API_PATH + ".yml";
        var endpointPath = "paths.'%s'.get".formatted(ymlPath);
        var contentPath = "paths.'%s'.get.responses.200.content".formatted(ymlPath);
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey(ymlPath))
                .body(endpointPath + ".operationId", Matchers.equalTo("getOpenAPISpecificationYml"))
                .body(contentPath, Matchers.hasKey("application/yaml"))
                .body(contentPath + ".size()", Matchers.equalTo(1));
    }

    @Test
    public void testSpecificJsonEndpointFilterResource() {
        var jsonPath = OPEN_API_PATH + ".json";
        var endpointPath = "paths.'%s'.get".formatted(jsonPath);
        var contentPath = "paths.'%s'.get.responses.200.content".formatted(jsonPath);
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey(jsonPath))
                .body(endpointPath + ".operationId", Matchers.equalTo("getOpenAPISpecificationJson"))
                .body(contentPath, Matchers.hasKey("application/json"))
                .body(contentPath + ".size()", Matchers.equalTo(1));
    }
}
