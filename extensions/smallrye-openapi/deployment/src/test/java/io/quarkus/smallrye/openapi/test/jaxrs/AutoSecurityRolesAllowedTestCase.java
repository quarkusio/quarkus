package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AutoSecurityRolesAllowedTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean.class, OpenApiResourceSecuredAtClassLevel.class,
                            OpenApiResourceSecuredAtMethodLevel.class, OpenApiResourceSecuredAtMethodLevel2.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=jwt\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=JWTCompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=JWT Authentication"),

                            "application.properties"));

    @Test
    public void testAutoSecurityRequirement() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("type", "http"))
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication",
                        Matchers.hasEntry("description", "JWT Authentication"))
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("scheme", "bearer"))
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication", Matchers.hasEntry("bearerFormat", "JWT"))
                .and()
                .body("paths.'/resource2/test-security/annotated'.get.security.JWTCompanyAuthentication",
                        Matchers.notNullValue())
                .and()
                .body("paths.'/resource2/test-security/naked'.get.security.JWTCompanyAuthentication", Matchers.notNullValue())
                .and()
                .body("paths.'/resource2/test-security/classLevel/1'.get.security.JWTCompanyAuthentication",
                        Matchers.notNullValue())
                .and()
                .body("paths.'/resource2/test-security/classLevel/2'.get.security.JWTCompanyAuthentication",
                        Matchers.notNullValue())
                .and()
                .body("paths.'/resource2/test-security/classLevel/3'.get.security.MyOwnName",
                        Matchers.notNullValue())
                .and()
                .body("paths.'/resource3/test-security/annotated'.get.security.AtClassLevel", Matchers.notNullValue());

    }

    @Test
    public void testOpenAPIAnnotations() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("paths.'/resource2/test-security/classLevel/1'.get.responses.401.description",
                        Matchers.equalTo("Not Authorized"))
                .and()
                .body("paths.'/resource2/test-security/classLevel/1'.get.responses.403.description",
                        Matchers.equalTo("Not Allowed"))
                .and()
                .body("paths.'/resource2/test-security/classLevel/2'.get.responses.401.description",
                        Matchers.equalTo("Not Authorized"))
                .and()
                .body("paths.'/resource2/test-security/classLevel/2'.get.responses.403.description",
                        Matchers.equalTo("Not Allowed"))
                .and()
                .body("paths.'/resource2/test-security/classLevel/3'.get.responses.401.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/classLevel/3'.get.responses.403.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/classLevel/4'.get.responses.401.description",
                        Matchers.equalTo("Who are you?"))
                .and()
                .body("paths.'/resource2/test-security/classLevel/4'.get.responses.403.description",
                        Matchers.equalTo("You cannot do that."))
                .and()
                .body("paths.'/resource2/test-security/naked'.get.responses.401.description",
                        Matchers.equalTo("Not Authorized"))
                .and()
                .body("paths.'/resource2/test-security/naked'.get.responses.403.description",
                        Matchers.equalTo("Not Allowed"))
                .and()
                .body("paths.'/resource2/test-security/annotated'.get.responses.401.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/annotated'.get.responses.403.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/methodLevel/1'.get.responses.401.description",
                        Matchers.equalTo("Not Authorized"))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/1'.get.responses.403.description",
                        Matchers.equalTo("Not Allowed"))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/2'.get.responses.401.description",
                        Matchers.equalTo("Not Authorized"))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/2'.get.responses.403.description",
                        Matchers.equalTo("Not Allowed"))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/public'.get.responses.401.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/methodLevel/public'.get.responses.403.description",
                        Matchers.nullValue())
                .and()
                .body("paths.'/resource2/test-security/annotated/documented'.get.responses.401.description",
                        Matchers.equalTo("Who are you?"))
                .and()
                .body("paths.'/resource2/test-security/annotated/documented'.get.responses.403.description",
                        Matchers.equalTo("You cannot do that."))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/3'.get.responses.401.description",
                        Matchers.equalTo("Who are you?"))
                .and()
                .body("paths.'/resource2/test-security/methodLevel/3'.get.responses.403.description",
                        Matchers.equalTo("You cannot do that."));
    }

}
