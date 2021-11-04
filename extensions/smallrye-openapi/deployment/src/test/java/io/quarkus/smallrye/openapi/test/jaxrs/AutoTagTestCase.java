package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AutoTagTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResourceWithNoTag.class));

    @Test
    public void testAutoSecurityRequirement() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("paths.'/resource/annotated'.get.tags", Matchers.hasItem("From Annotation"))
                .and()
                .body("paths.'/resource/auto'.get.tags", Matchers.hasItem("Open Api Resource With No Tag"))
                .and()
                .body("paths.'/resource/auto'.post.tags", Matchers.hasItem("Open Api Resource With No Tag"));

    }

}
