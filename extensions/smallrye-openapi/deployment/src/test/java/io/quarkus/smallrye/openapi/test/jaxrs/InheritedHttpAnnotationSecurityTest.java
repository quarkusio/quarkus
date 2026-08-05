package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matcher;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

class InheritedHttpAnnotationSecurityTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BaseResource.class, ChildResource.class,
                            SimpleBaseResource.class, SimpleChildResource.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=jwt\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=JWTAuth\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=JWT Authentication"),
                            "application.properties"));

    static Matcher<Iterable<Object>> securityScheme(String schemeName, String... roles) {
        return allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        aMapWithSize(1),
                        hasEntry(equalTo(schemeName), containsInAnyOrder(roles)))));
    }

    @Test
    void testInheritedHttpAnnotationWithSecurity() {
        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .body("components.securitySchemes.JWTAuth", allOf(
                        hasEntry("type", "http"),
                        hasEntry("scheme", "bearer"),
                        hasEntry("bearerFormat", "JWT"),
                        hasEntry("description", "JWT Authentication")))
                .and()
                // Method with @RolesAllowed("admin") overriding parent @GET method should have security
                .body("paths.'/child/list'.get.security", securityScheme("JWTAuth", "admin"))
                .and()
                // Method with @RolesAllowed("user") overriding parent @POST method should have security
                .body("paths.'/child/create'.post.security", securityScheme("JWTAuth", "user"))
                .and()
                // Method without @RolesAllowed overriding parent @GET method should not have security
                .body("paths.'/child/count'.get.security", nullValue())
                .and()
                // Simple test without generics
                .body("paths.'/simple-child/simple'.get.security", securityScheme("JWTAuth", "admin"));
    }
}
