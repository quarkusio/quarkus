package io.quarkus.it.oidc.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class OidcTokenPropagationGraphQLTest {

    final KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void testAccessTokenPropagation() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice"))
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
