package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcTokenPropagationTest {

    @Test
    // TODO - MP4 - Require SR JWT 3.0.1
    @Disabled
    public void testGetUserNameWithJwtTokenPropagation() {
        RestAssured.given().auth().oauth2(KeycloakRealmResourceManager.getAccessToken("alice"))
                .when().get("/frontend/jwt-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    // TODO - MP4 - Require SR JWT 3.0.1
    @Disabled
    public void testGetUserNameWithAccessTokenPropagation() {
        RestAssured.given().auth().oauth2(KeycloakRealmResourceManager.getAccessToken("alice"))
                .when().get("/frontend/access-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
