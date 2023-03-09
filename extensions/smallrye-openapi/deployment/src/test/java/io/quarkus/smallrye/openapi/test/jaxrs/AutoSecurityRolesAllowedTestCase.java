package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoSecurityRolesAllowedTestCase {

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

    static Matcher<Iterable<Object>> schemeArray(String schemeName) {
        return allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        aMapWithSize(1),
                        hasEntry(equalTo(schemeName), emptyIterable()))));
    }

    @Test
    void testAutoSecurityRequirement() {
        var defaultSecurity = schemeArray("JWTCompanyAuthentication");

        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.JWTCompanyAuthentication", allOf(
                        hasEntry("type", "http"),
                        hasEntry("scheme", "bearer"),
                        hasEntry("bearerFormat", "JWT"),
                        hasEntry("description", "JWT Authentication")))
                .and()
                // OpenApiResourceSecuredAtMethodLevel
                .body("paths.'/resource2/test-security/naked'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/annotated'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/methodLevel/1'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/methodLevel/2'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/methodLevel/public'.get.security", nullValue())
                .body("paths.'/resource2/test-security/annotated/documented'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/methodLevel/3'.get.security", defaultSecurity)
                .and()
                // OpenApiResourceSecuredAtClassLevel
                .body("paths.'/resource2/test-security/classLevel/1'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/classLevel/2'.get.security", defaultSecurity)
                .body("paths.'/resource2/test-security/classLevel/3'.get.security", schemeArray("MyOwnName"))
                .body("paths.'/resource2/test-security/classLevel/4'.get.security", defaultSecurity)
                .and()
                // OpenApiResourceSecuredAtMethodLevel2
                .body("paths.'/resource3/test-security/annotated'.get.security", schemeArray("AtClassLevel"));
    }

    @Test
    void testOpenAPIAnnotations() {
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
