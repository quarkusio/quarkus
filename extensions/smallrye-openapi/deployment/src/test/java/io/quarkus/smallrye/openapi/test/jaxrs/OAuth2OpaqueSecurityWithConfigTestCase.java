package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OAuth2OpaqueSecurityWithConfigTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=oauth2\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=OAuth2CompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=OAuth2 Authentication"),

                            "application.properties"));

    @Test
    public void testOAuth2Authentication() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then().body("components.securitySchemes.OAuth2CompanyAuthentication", Matchers.hasEntry("type", "http"))
                .and()
                .body("components.securitySchemes.OAuth2CompanyAuthentication",
                        Matchers.hasEntry("description", "OAuth2 Authentication"))
                .and().body("components.securitySchemes.OAuth2CompanyAuthentication", Matchers.hasEntry("scheme", "bearer"))
                .and()
                .body("components.securitySchemes.OAuth2CompanyAuthentication", Matchers.hasEntry("bearerFormat", "Opaque"));
    }

}
