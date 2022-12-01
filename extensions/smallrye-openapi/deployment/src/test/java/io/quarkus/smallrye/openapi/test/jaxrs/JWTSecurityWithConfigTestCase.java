package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JWTSecurityWithConfigTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=jwt\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=JWTCompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=JWT Authentication"),

                            "application.properties"));

    @Test
    public void testJWTAuthentication() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then().body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("type", "http"))
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication",
                        Matchers.hasEntry("description", "JWT Authentication"))
                .and().body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("scheme", "bearer"))
                .and().body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("bearerFormat", "JWT"));
    }

}
