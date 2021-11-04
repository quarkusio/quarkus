package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class BasicSecurityWithConfigTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=basic\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=CompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=Basic Authentication"),

                            "application.properties"));

    @Test
    public void testBasicAuthentication() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then().body("components.securitySchemes.CompanyAuthentication", Matchers.hasEntry("type", "http"))
                .and()
                .body("components.securitySchemes.CompanyAuthentication",
                        Matchers.hasEntry("description", "Basic Authentication"))
                .and().body("components.securitySchemes.CompanyAuthentication", Matchers.hasEntry("scheme", "basic"));
    }

}
