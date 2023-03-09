package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matcher;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoSecurityRolesAllowedWithScopesTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean.class, OpenApiResourceSecuredAtClassLevel.class,
                            OpenApiResourceSecuredAtMethodLevel.class, OpenApiResourceSecuredAtMethodLevel2.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=oauth2-implicit\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=OAuth2\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=OAuth2 Authentication"),

                            "application.properties"));

    static Matcher<Iterable<Object>> schemeArray(String schemeName, Matcher<Iterable<?>> scopesMatcher) {
        return allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        aMapWithSize(1),
                        hasEntry(equalTo(schemeName), scopesMatcher))));
    }

    @Test
    void testAutoSecurityRequirement() {
        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.OAuth2", allOf(
                        hasEntry("type", "oauth2"),
                        hasEntry("description", "OAuth2 Authentication")))
                .and()
                // OpenApiResourceSecuredAtMethodLevel
                .body("paths.'/resource2/test-security/naked'.get.security", schemeArray("OAuth2", contains("admin")))
                .body("paths.'/resource2/test-security/annotated'.get.security",
                        schemeArray("JWTCompanyAuthentication", emptyIterable()))
                .body("paths.'/resource2/test-security/methodLevel/1'.get.security", schemeArray("OAuth2", contains("user1")))
                .body("paths.'/resource2/test-security/methodLevel/2'.get.security", schemeArray("OAuth2", contains("user2")))
                .body("paths.'/resource2/test-security/methodLevel/public'.get.security", nullValue())
                .body("paths.'/resource2/test-security/annotated/documented'.get.security",
                        schemeArray("JWTCompanyAuthentication", emptyIterable()))
                .body("paths.'/resource2/test-security/methodLevel/3'.get.security", schemeArray("OAuth2", contains("admin")))
                .and()
                // OpenApiResourceSecuredAtClassLevel
                .body("paths.'/resource2/test-security/classLevel/1'.get.security", schemeArray("OAuth2", contains("user1")))
                .body("paths.'/resource2/test-security/classLevel/2'.get.security", schemeArray("OAuth2", contains("user2")))
                .body("paths.'/resource2/test-security/classLevel/3'.get.security", schemeArray("MyOwnName", emptyIterable()))
                .body("paths.'/resource2/test-security/classLevel/4'.get.security", schemeArray("OAuth2", contains("admin")))
                .and()
                // OpenApiResourceSecuredAtMethodLevel2
                .body("paths.'/resource3/test-security/annotated'.get.security",
                        schemeArray("AtClassLevel", emptyIterable()));
    }

}
