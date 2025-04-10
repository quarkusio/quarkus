package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AutoAddOpenApiEndpointFilterTest {
    private static final String OPEN_API_PATH = "/openapi.json";
    private static final String JSON_PATH_FOR_ENDPOINT = "paths.'%s'.get".formatted(OPEN_API_PATH);

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
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey(OPEN_API_PATH))
                .body(JSON_PATH_FOR_ENDPOINT + ".tags", Matchers.hasItem("openapi"))
                .body(JSON_PATH_FOR_ENDPOINT + ".responses", Matchers.hasKey("200"))
                .body(JSON_PATH_FOR_ENDPOINT + ".responses.200.content", Matchers.hasKey("application/json"))
                .body("tags", Matchers.hasItem(Matchers.hasEntry("name", "openapi")));
    }

}
