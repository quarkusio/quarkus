package io.quarkus.it.oidc.dev.services;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OidcGraphQLClientTest {

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
}
