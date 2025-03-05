package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.notNullValue;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoSecurityAuthenticateTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean2.class, OpenApiResourceAuthenticatedAtClassLevel.class,
                            OpenApiResourceAuthenticatedInherited1.class, OpenApiResourceAuthenticatedInherited2.class,
                            OpenApiResourceAuthenticatedAtMethodLevel.class, OpenApiResourceAuthenticatedAtMethodLevel2.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-openapi.security-scheme=jwt
                                    quarkus.smallrye-openapi.security-scheme-name=JWTCompanyAuthentication
                                    quarkus.smallrye-openapi.security-scheme-description=JWT Authentication
                                    """),
                            "application.properties"));

    @Test
    void testAutoSecurityRequirement() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication", Matchers.allOf(
                        Matchers.hasEntry("type", "http"),
                        Matchers.hasEntry("description", "JWT Authentication"),
                        Matchers.hasEntry("scheme", "bearer"),
                        Matchers.hasEntry("bearerFormat", "JWT")))
                .body("paths.'/resource2/test-security/annotated'.get.security.JWTCompanyAuthentication", notNullValue())
                .body("paths.'/resource2/test-security/naked'.get.security.JWTCompanyAuthentication", notNullValue())
                .body("paths.'/resource2/test-security/classLevel/1'.get.security.JWTCompanyAuthentication", notNullValue())
                .body("paths.'/resource2/test-security/classLevel/2'.get.security.JWTCompanyAuthentication", notNullValue())
                .body("paths.'/resource2/test-security/classLevel/3'.get.security.MyOwnName", notNullValue())
                .body("paths.'/resource3/test-security/annotated'.get.security.AtClassLevel", notNullValue())
                .body("paths.'/resource-inherited1/test-security/classLevel/1'.get.security.JWTCompanyAuthentication",
                        notNullValue())
                .body("paths.'/resource-inherited1/test-security/classLevel/2'.get.security.JWTCompanyAuthentication",
                        notNullValue())
                .body("paths.'/resource-inherited1/test-security/classLevel/3'.get.security.MyOwnName", notNullValue())
                .body("paths.'/resource-inherited2/test-security/classLevel/1'.get.security.JWTCompanyAuthentication",
                        notNullValue())
                .body("paths.'/resource-inherited2/test-security/classLevel/2'.get.security.CustomOverride",
                        notNullValue())
                .body("paths.'/resource-inherited2/test-security/classLevel/3'.get.security.MyOwnName", notNullValue());

    }

}
