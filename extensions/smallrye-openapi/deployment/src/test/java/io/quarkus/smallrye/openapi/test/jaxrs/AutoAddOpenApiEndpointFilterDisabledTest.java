package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AutoAddOpenApiEndpointFilterDisabledTest {
    private static final String OPEN_API_PATH = "/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-openapi.auto-add-open-api-endpoint=false
                                    quarkus.smallrye-openapi.path=%s
                                    """.formatted(OPEN_API_PATH)),

                            "application.properties"));

    @Test
    public void testNoAddedFilterDoesNotProducePath() {
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.not(Matchers.hasKey(OPEN_API_PATH)));
    }

}
