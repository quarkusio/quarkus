package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoTagTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(OpenApiResourceWithNoTag.class, AutoTagResource.class,
                    AutoTagFetchableResource.class, AbstractAutoTagResource.class));

    @Test
    void testTagInOpenApi() {
        RestAssured.given().header("Accept", "application/json").when().get("/q/openapi").then().log()
                .ifValidationFails().assertThat().statusCode(200)
                .body("paths.'/tagged'.get.tags", Matchers.hasItem("Auto Tag Resource"))
                .body("paths.'/tagged/{id}'.get.tags", Matchers.hasItem("Auto Tag Resource"))
                .body("paths.'/resource/annotated'.get.tags", Matchers.hasItem("From Annotation"))
                .body("paths.'/resource/auto'.get.tags", Matchers.hasItem("Open Api Resource With No Tag"))
                .body("paths.'/resource/auto'.post.tags", Matchers.hasItem("Open Api Resource With No Tag"));
    }

}
