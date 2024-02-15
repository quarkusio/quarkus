package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiAnnotatedCounterBuildtimeFilterTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class, CounterBuildtimeFilter.class));

    @Test
    public void testOpenApiFilterResource() {
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("info.description", Matchers.startsWith("CounterBuildtimeFilter was called 1 time(s)"));

    }

}
