package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultContentTypeTest {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DefaultContentTypeResource.class, Greeting.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.store-schema-directory=target"),
                            "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths.'/greeting/goodbye'.get.responses.'200'.content.'application/xml'.schema.$ref",
                        Matchers.containsString("#/components/schemas/Greeting"))
                .body("paths.'/greeting/hello'.get.responses.'200'.content.'application/json'.schema.$ref",
                        Matchers.containsString("#/components/schemas/Greeting"))
                .body("paths.'/greeting/hello'.post.responses.'200'.content.'application/json'.schema.$ref",
                        Matchers.containsString("#/components/schemas/Greeting"));

    }
}
