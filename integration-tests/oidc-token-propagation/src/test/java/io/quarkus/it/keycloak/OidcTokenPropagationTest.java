package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcTokenPropagationTest {

    @Test
    public void testGetUserNameOidcClient() {
        RestAssured.given().auth().oauth2(KeycloakRealmResourceManager.getAccessToken("alice"))
                .when().get("/frontend/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
