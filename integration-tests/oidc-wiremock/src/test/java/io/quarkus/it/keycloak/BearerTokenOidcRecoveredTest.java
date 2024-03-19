package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
public class BearerTokenOidcRecoveredTest {

    @Order(1)
    @Test
    public void testOidcRecoveredWithDiscovery() {
        String token = getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

        // Server has not started
        RestAssured.given().auth().oauth2(token)
                .when().get("/recovered/api/users/preferredUserName")
                .then()
                .statusCode(500);

        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            RestAssured.given().auth().oauth2(token)
                    .when().get("/recovered/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo("alice"));
        } finally {
            server.stop();
        }
    }

    @Order(2)
    @Test
    public void testOidcRecoveredWithNoDiscovery() {
        String token = getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

        // Server has not started
        RestAssured.given().auth().oauth2(token)
                .when().get("/recovered-no-discovery/api/users/preferredUserName")
                .then()
                .statusCode(500);

        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            RestAssured.given().auth().oauth2(token)
                    .when().get("/recovered-no-discovery/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo("alice"));
        } finally {
            server.stop();
        }
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws()
                .keyId("1")
                .sign();
    }

    @Order(3)
    @Test
    public void assertOidcServerAvailabilityReported() {
        String expectAuthServerUrl = RestAssured.get("/oidc-event/expected-auth-server-url").then().statusCode(200).extract()
                .asString();
        RestAssured.given().get("/oidc-event/unavailable-auth-server-urls").then().statusCode(200)
                .body(Matchers.is(expectAuthServerUrl));
        RestAssured.given().get("/oidc-event/available-auth-server-urls").then().statusCode(200)
                .body(Matchers.is(expectAuthServerUrl));
    }
}
