package io.quarkus.it.spring.web.openapi;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * This test is a reproducer for https://github.com/quarkusio/quarkus/issues/4613.
 */
public class SwaggerAndOpenAPIWithCommonPrefixPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OpenApiController.class)
                    .addAsResource("test-roles.properties")
                    .addAsResource("test-users.properties"))
            .overrideConfigKey("quarkus.smallrye-openapi.path", "swagger")
            .overrideConfigKey("quarkus.swagger-ui.always-include", "true")
            .setRun(true);

    @Test
    public void shouldWorkEvenWithCommonPrefix() {
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/q/swagger"));
        RestAssured.when().get("/q/swagger").then().statusCode(200)
                .body(containsString("/resource"), containsString("QUERY_PARAM_1"));
    }
}
