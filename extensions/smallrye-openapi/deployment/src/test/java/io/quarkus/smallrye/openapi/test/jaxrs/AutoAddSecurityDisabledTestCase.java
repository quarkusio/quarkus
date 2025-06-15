package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoAddSecurityDisabledTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(OpenApiResource.class, ResourceBean.class).addAsResource(
                    new StringAsset("quarkus.smallrye-openapi.auto-add-security=false\n"), "application.properties"));

    @Test
    void testAutoSecurityRequirement() {
        RestAssured.given().header("Accept", "application/json").when().get("/q/openapi").then().log()
                .ifValidationFails().body("components", not(hasKey(equalTo("securitySchemes"))));
    }

}
