package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.OpenApiFilter.RunStage;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.test.QuarkusUnitTest;

class OpenApiRunStagePrecedenceOverValueTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(StagesPrecedenceFilter.class));

    @OpenApiFilter(value = RunStage.RUN, stages = RunStage.BUILD)
    public static class StagesPrecedenceFilter implements OASFilter {
        public static int invocationCount = 0;

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            invocationCount++;
            openAPI.addExtension("x-stages-precedence", "present");
        }
    }

    @Test
    public void testStagesTakesPrecedenceOverValue() throws IOException {
        String buildTimeDocument = new String(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(OpenApiConstants.BASE_NAME + ".JSON").readAllBytes(), StandardCharsets.UTF_8);
        assertThat(buildTimeDocument, containsString("x-stages-precedence"));

        assertThat(StagesPrecedenceFilter.invocationCount, is(0));

        given()
                .when().get("/q/openapi.json")
                .then()
                .statusCode(200)
                .body("x-stages-precedence", org.hamcrest.Matchers.equalTo("present"));

        assertThat(StagesPrecedenceFilter.invocationCount, is(0));
    }
}
