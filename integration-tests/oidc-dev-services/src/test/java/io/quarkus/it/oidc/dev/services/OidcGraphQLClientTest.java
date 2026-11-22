package io.quarkus.it.oidc.dev.services;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class OidcGraphQLClientTest {

    static final OidcTestClient oidcTestClient = new OidcTestClient();

    @AfterAll
    public static void close() {
        oidcTestClient.close();
    }

    @Test
    void defaultOidcClient() {
        RestAssured.when().get("/oidc-graphql-test/default")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    void namedOidcClient() {
        RestAssured.when().get("/oidc-graphql-test/named")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    void tokenPropagation() {
        // OIDC clients acquire tokens for 'alice', so 'bob' is only returned if the incoming token is propagated
        RestAssured.given()
                .auth().oauth2(oidcTestClient.getAccessToken("bob", "bob"))
                .when().get("/oidc-graphql-test/token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));
    }

    @Test
    void tokenExchangeAndPropagation() {
        int tokenExchanges = getTokenExchanges();
        String accessToken = oidcTestClient.getAccessToken("bob", "bob");
        for (int i = 0; i < 2; i++) {
            RestAssured.given()
                    .auth().oauth2(accessToken)
                    .when().get("/oidc-graphql-test/token-exchange")
                    .then()
                    .statusCode(200)
                    .body(equalTo("bob"));
        }
        // the access token is exchanged once, then the exchanged token is cached
        assertEquals(tokenExchanges + 1, getTokenExchanges());
    }

    private static int getTokenExchanges() {
        return Integer.parseInt(RestAssured.when().get("/oidc-graphql-test/token-exchanges")
                .then()
                .statusCode(200)
                .extract().asString());
    }
}
