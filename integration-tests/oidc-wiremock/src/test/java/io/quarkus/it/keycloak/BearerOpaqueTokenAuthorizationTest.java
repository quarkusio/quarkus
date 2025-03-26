package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class BearerOpaqueTokenAuthorizationTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @Test
    public void testSecureAccessSuccessPreferredUsername() {
        for (String username : Arrays.asList("alice", "admin")) {
            RestAssured.given()
                    .header("Authorization", "Bearer " + username)
                    .when().get("/opaque/api/users/preferredUserName/bearer")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given()
                .header("Authorization", "Bearer " + "admin")
                .when().get("/opaque/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given()
                .header("Authorization", "Bearer " + "alice")
                .when().get("/opaque/api/admin/bearer")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDeniedNoBearerToken() {
        RestAssured.given()
                .when().get("/opaque/api/users/me/bearer").then()
                .statusCode(401);
    }

    @Test
    public void testExpiredBearerToken() {

        RestAssured.given()
                .header("Authorization", "Bearer " + "expired")
                .get("/opaque/api/users/me/bearer")
                .then()
                .statusCode(401)
                .body(Matchers.equalTo("Token: expired"));
    }

    @Test
    public void testGitHubBearerTokenSuccess() {
        final String validToken = OidcConstants.BEARER_SCHEME + " ghu_XirRniLaPuW53pDylNnAPOPBm14taM0C9HP4";
        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/github/userinfo"))
                        .withHeader("Authorization", matching(validToken))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "      \"preferred_username\": \"alice\""
                                        + "}")));

        RestAssured.given()
                .header("Authorization", validToken)
                .get("/bearer-user-info-github-service")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("alice:alice:ghu_XirRniLaPuW53pDylNnAPOPBm14taM0C9HP4"));
    }

    @Test
    public void testGitHubBearerTokenUnauthorized() {
        final String invalidToken = OidcConstants.BEARER_SCHEME + " Invalid";
        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/github/userinfo"))
                        .withHeader("Authorization", matching(invalidToken))
                        .willReturn(aResponse().withStatus(401)));

        RestAssured.given()
                .header("Authorization", invalidToken)
                .get("/bearer-user-info-github-service")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGitHubBearerTokenNullUserInfo() {
        final String validToken = OidcConstants.BEARER_SCHEME + " ghu_AAAAniLaPuW53pDylNnAPOPBm14ta7777777";
        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/github/userinfo"))
                        .withHeader("Authorization", matching(validToken))
                        .willReturn(aResponse().withStatus(200).withBody((String) null)));

        RestAssured.given()
                .header("Authorization", validToken)
                .get("/bearer-user-info-github-service")
                .then()
                .statusCode(401);
    }

}
