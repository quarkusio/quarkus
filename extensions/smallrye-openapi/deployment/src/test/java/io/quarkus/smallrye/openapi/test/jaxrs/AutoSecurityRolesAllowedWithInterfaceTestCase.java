package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoSecurityRolesAllowedWithInterfaceTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ApplicationContext.class,
                            FooAPI.class, FooResource.class));

    static Matcher<Iterable<Object>> schemeArray(String schemeName, String... roles) {
        return allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        aMapWithSize(1),
                        hasEntry(equalTo(schemeName), containsInAnyOrder(roles)))));
    }

    @Test
    void testAutoSecurityRequirement() {

        var oidcAuth = schemeArray("oidc_auth", "RoleXY");

        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().body()
                .and()
                .body("components.securitySchemes.oidc_auth.$ref", equalToObject("#/components/securitySchemes/oidc_auth"))
                .and()
                .body("paths.'/secured/foo'.get.security", oidcAuth);

    }

}
