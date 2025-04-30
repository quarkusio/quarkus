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

import io.restassured.RestAssured;

abstract class AutoSecurityRolesAllowedWithScopesTestBase {

    static Matcher<Iterable<Object>> schemeArray(String schemeName, Matcher<Iterable<?>> scopesMatcher) {
        return allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        aMapWithSize(1),
                        hasEntry(equalTo(schemeName), scopesMatcher))));
    }

    void testAutoSecurityRequirement(String schemeType) {
        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.MyScheme", allOf(
                        hasEntry("type", schemeType),
                        hasEntry("description", "Authentication using MyScheme")))
                .and()
                // OpenApiResourceSecuredAtMethodLevel
                .body("paths.'/resource2/test-security/naked'.get.security", schemeArray("MyScheme", contains("admin")))
                .body("paths.'/resource2/test-security/annotated'.get.security",
                        schemeArray("JWTCompanyAuthentication", emptyIterable()))
                .body("paths.'/resource2/test-security/methodLevel/1'.get.security", schemeArray("MyScheme", contains("user1")))
                .body("paths.'/resource2/test-security/methodLevel/2'.get.security", schemeArray("MyScheme", contains("user2")))
                .body("paths.'/resource2/test-security/methodLevel/public'.get.security", nullValue())
                .body("paths.'/resource2/test-security/annotated/documented'.get.security",
                        schemeArray("JWTCompanyAuthentication", emptyIterable()))
                .body("paths.'/resource2/test-security/methodLevel/3'.get.security", schemeArray("MyScheme", contains("admin")))
                .and()
                // OpenApiResourceSecuredAtClassLevel
                .body("paths.'/resource2/test-security/classLevel/1'.get.security", schemeArray("MyScheme", contains("user1")))
                .body("paths.'/resource2/test-security/classLevel/2'.get.security", schemeArray("MyScheme", contains("user2")))
                .body("paths.'/resource2/test-security/classLevel/3'.get.security", schemeArray("MyOwnName", emptyIterable()))
                .body("paths.'/resource2/test-security/classLevel/4'.get.security", schemeArray("MyScheme", contains("admin")))
                .and()
                // OpenApiResourceSecuredAtMethodLevel2
                .body("paths.'/resource3/test-security/annotated'.get.security",
                        schemeArray("AtClassLevel", emptyIterable()));
    }

}
