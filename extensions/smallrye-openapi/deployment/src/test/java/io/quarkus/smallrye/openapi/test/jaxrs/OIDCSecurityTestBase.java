package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

abstract class OIDCSecurityTestBase {

    @Test
    void testOIDCAuthentication() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then().body("components.securitySchemes.OIDCCompanyAuthentication",
                        allOf(
                                hasEntry("type", "openIdConnect"),
                                hasEntry("description", "OIDC Authentication"),
                                hasEntry("openIdConnectUrl",
                                        "http://localhost:8081/auth/realms/OpenAPIOIDC/.well-known/openid-configuration")));
    }

}
