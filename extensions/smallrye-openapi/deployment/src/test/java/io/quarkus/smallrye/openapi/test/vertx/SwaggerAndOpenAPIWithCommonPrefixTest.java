package io.quarkus.smallrye.openapi.test.vertx;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * This test is a reproducer for https://github.com/quarkusio/quarkus/issues/4613.
 */
public class SwaggerAndOpenAPIWithCommonPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.path=swagger"), "application.properties"));

    @Test
    public void shouldWorkEvenWithCommonPrefix() {
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/q/swagger"));
        RestAssured.when().get("/q/swagger").then().statusCode(200)
                .body(containsString("/resource"), containsString("QUERY_PARAM_1"));
    }
}
