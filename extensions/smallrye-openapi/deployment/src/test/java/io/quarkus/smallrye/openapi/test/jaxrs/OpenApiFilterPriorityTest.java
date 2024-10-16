package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiFilterPriorityTest {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class,
                            MyBuildTimeFilterPrioDefault.class, MyBuildTimeFilterPrio2.class, MyBuildTimeFilterPrio0.class));

    @Test
    public void shouldApplyFilterSortedByPriority() {
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("info.description", Matchers.equalTo("210"));

    }

}
