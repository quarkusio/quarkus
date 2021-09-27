package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcTokenPropagationTest {

    final KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void testGetUserNameWithJwtTokenPropagation() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/frontend/jwt-token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameWithAccessTokenPropagation() {
        // At the moment it is not possible to configure Keycloak Token Exchange permissions
        // vi the admin API or export the realm with such permissions.
        // So at this stage this test only verifies that as far as the token propagation is concerned
        // the exchange grant request is received by Keycloak as per the test configuration.

        // Note this test does pass if Keycloak is started manually, 
        // 'quarkus' realm, 'quarkus-app' and 'quarkus-app-exchange' clients, and 'alice' user is created 
        // and the token-exchange permission is added to the clients as per the Keycloak docs.
        // It can be confirmed by commenting @QuarkusTestResource above
        // and running the tests as 'mvn clean install -Dtest-containers'

        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/frontend/access-token-propagation")
                .then()
                //.statusCode(200)
                //.body(equalTo("alice"));
                .statusCode(500)
                .body(containsString("Client not allowed to exchange"));
    }

    @Test
    public void testGetUserNameFromServiceAccount() {
        RestAssured.when().get("/frontend/service-account")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    public String getAccessToken(String userName) {
        return client.getAccessToken(userName, userName, "quarkus-app", "secret");
    }
}
