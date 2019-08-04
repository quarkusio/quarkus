package io.quarkus.it.elytron.oauth2;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class ElytronOauth2ExtensionResourceTestCase {

    private static final String BEARER_TOKEN = "337aab0f-b547-489b-9dbd-a54dc7bdf20d";

    private static WireMockServer wireMockServer = new WireMockServer();

    @BeforeAll
    static void start() {
        wireMockServer.start();

        // define the mock for the introspect endpoint
        WireMock.stubFor(WireMock.post("/introspect").willReturn(WireMock.aResponse()
                .withBody(
                        "{\"active\":true,\"scope\":\"READER\",\"username\":null,\"iat\":1562315654,\"exp\":1562317454,\"expires_in\":1458,\"client_id\":\"my_client_id\"}")));
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Test
    void anonymous() {
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    void authenticated() {
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/authenticated")
                .then()
                .statusCode(200)
                .body(containsString("authenticated"));
    }

    @Test
    void authenticated_not_authenticated() {
        RestAssured.given()
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(401);
    }

    @Test
    void forbidden() {
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/forbidden")
                .then()
                .statusCode(403);
    }

    @Test
    void forbidden_not_authenticated() {
        RestAssured.given()
                .when()
                .get("/api/forbidden")
                .then()
                .statusCode(401);
    }
}
