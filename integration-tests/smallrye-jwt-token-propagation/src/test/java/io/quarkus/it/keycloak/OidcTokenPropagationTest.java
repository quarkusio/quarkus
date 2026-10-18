package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.RolesSecurityIdentityAugmentor.USE_SEC_IDENTITY_AUGMENTOR;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcTokenPropagationTest {

    final KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void testGetUserNameWithJwtTokenPropagation() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice"))
                .when().get("/frontend/jwt-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameWithJwtTokenPropagationAndAugmentedIdentity() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice"))
                .header(USE_SEC_IDENTITY_AUGMENTOR, Boolean.TRUE)
                .when().get("/frontend/jwt-token-propagation-with-augmentation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameWithAccessTokenPropagation() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice"))
                .when().get("/frontend/access-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testEchoUserNameWithAccessTokenPropagation() {
        RestAssured.given().contentType(ContentType.JSON).auth().oauth2(client.getAccessToken("alice"))
                .when().body("{\"name\":\"alice\"}").post("/frontend/access-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice:alice"));
    }

    @Test
    public void testEchoUserNameWithAccessTokenPropagationForbidden() {
        RestAssured.given().contentType(ContentType.JSON).auth().oauth2(client.getAccessToken("john"))
                .when().body("{\"name\":\"alice\"}").post("/frontend/access-token-propagation")
                .then()
                .statusCode(403);
    }
}
