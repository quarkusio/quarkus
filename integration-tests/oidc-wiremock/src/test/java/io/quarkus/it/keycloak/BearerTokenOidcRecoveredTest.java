package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

        checkHealth("DOWN", "Not Ready");

        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            RestAssured.given().auth().oauth2(token)
                    .when().get("/recovered/api/users/preferredUserName")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo("alice"));

            checkHealth("UP", "OK");
        } finally {
            server.stop();
        }
    }

    private static void checkHealth(String expectedCheckStatus, String expectedDefaultTenantStatus) {
        Response healthReadyResponse = RestAssured.when().get("http://localhost:8081/q/health/ready");

        JsonObject jsonHealth = new JsonObject(healthReadyResponse.asString());
        JsonObject oidcCheck = getOidcCheck(jsonHealth.getJsonArray("checks"));

        assertEquals(expectedCheckStatus, oidcCheck.getString("status"));

        JsonObject data = oidcCheck.getJsonObject("data");
        assertEquals(expectedDefaultTenantStatus, data.getString("Default"));

        assertEquals("Unknown", data.getString("bearer-user-info-github-service"));
    }

    private static JsonObject getOidcCheck(JsonArray checks) {
        // GRPC and OIDC checks are available
        for (int i = 0; i < checks.size(); i++) {
            if ("OIDC Provider Health Check".equals(checks.getJsonObject(i).getString("name"))) {
                return checks.getJsonObject(i);
            }
        }
        return null;
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
