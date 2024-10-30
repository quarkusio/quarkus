package io.quarkus.smallrye.openapi.test.jaxrs;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiEmptyStaticModelsTestCase {

    private static String directory = "META-INF/foldernamethatdoesnotexist";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.additional-docs-directory=" + directory),
                            "application.properties"));

    @Test
    public void testNonExistingAdditionalDocsDirectory() throws IOException {

        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().body().and()
                .body("openapi", Matchers.startsWith("3.1."));
    }
}
