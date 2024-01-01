package io.quarkus.it.elytron.oauth2;

import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ElytronOauth2ExtensionResourceTestCase {

    private static final String BEARER_TOKEN = "337aab0f-b547-489b-9dbd-a54dc7bdf20d";

    private static WireMockServer wireMockServer;

    private static void ensureStarted() {
        if (wireMockServer != null) {
            return;
        }
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        // define the mock for the introspect endpoint
        WireMock.stubFor(WireMock.post("/introspect").willReturn(WireMock.aResponse()
                .withBody(
                        "{\"active\":true,\"scope\":\"READER\",\"username\":null,\"iat\":1562315654,\"exp\":1562317454,\"expires_in\":1458,\"client_id\":\"my_client_id\"}")));

    }

    @AfterAll
    public static void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void anonymous() {
        ensureStarted();
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    public void authenticated() {
        ensureStarted();
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/authenticated")
                .then()
                .statusCode(200)
                .body(containsString("authenticated"));
    }

    @Test
    public void authenticated_not_authenticated() {
        ensureStarted();
        RestAssured.given()
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(401);
    }

    @Test
    public void forbidden() {
        ensureStarted();
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/forbidden")
                .then()
                .statusCode(403);
    }

    @Test
    public void forbidden_not_authenticated() {
        ensureStarted();
        RestAssured.given()
                .when()
                .get("/api/forbidden")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGrpcAuthorization() {
        ensureStarted();
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/grpc-writer")
                .then()
                .statusCode(500);
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer: " + BEARER_TOKEN)
                .get("/api/grpc-reader")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Ron from null"));
    }
}
