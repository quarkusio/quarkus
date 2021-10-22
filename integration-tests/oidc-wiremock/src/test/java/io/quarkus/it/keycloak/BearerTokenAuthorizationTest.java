package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class BearerTokenAuthorizationTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @Test
    public void testSecureAccessSuccessPreferredUsername() {
        for (String username : Arrays.asList("alice", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username, Set.of("user", "admin")))
                    .when().get("/api/users/preferredUserName/bearer")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin")))
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testAccessAdminResourceWithCertThumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithThumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-no-introspection")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testSecureAccessSuccessPreferredUsernameWrongRolePath() {
        for (String username : Arrays.asList("alice", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username, Set.of("user", "admin")))
                    .when().get("/api/users/preferredUserName/bearer-wrong-role-path")
                    .then()
                    .statusCode(403);
        }
    }

    @Test
    public void testAccessAdminResourceWrongRolePath() {
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-wrong-role-path")
                .then()
                .statusCode(403);
    }

    @Test
    public void testAccessAdminResourceAudienceArray() {
        RestAssured.given().auth().oauth2(getAccessTokenAudienceArray("admin", Set.of("admin")))
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", Set.of("user")))
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDeniedNoBearerToken() {
        RestAssured.given()
                .when().get("/api/users/me/bearer").then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    @Test
    public void testExpiredBearerToken() {
        String token = getExpiredAccessToken("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    @Test
    public void testBearerTokenWrongIssuer() {
        String token = getAccessTokenWrongIssuer("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    @Test
    public void testBearerTokenWrongAudience() {
        String token = getAccessTokenWrongAudience("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    @Test
    public void testInvalidBearerToken() {
        wireMockServer.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token/introspect")
                .withRequestBody(matching(".*token=invalid_token.*"))
                .willReturn(WireMock.aResponse().withStatus(400)));

        RestAssured.given().auth().oauth2("invalid_token").when()
                .get("/api/users/me/bearer")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();
    }

    private String getAccessTokenWithThumbprint(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().thumbprint(OidcWiremockTestResource.getCertificate())
                .sign("privateKeyWithoutKid.jwk");
    }

    private String getAccessTokenWrongAudience(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://services.com")
                .sign();
    }

    private String getAccessTokenAudienceArray(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience(new HashSet<>(Arrays.asList("https://service.example.com", "https://frontendservice.example.com")))
                .sign();
    }

    private String getExpiredAccessToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .expiresAt(Instant.MIN)
                .sign();
    }

    private String getAccessTokenWrongIssuer(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://example.com")
                .audience("https://service.example.com")
                .sign();
    }

}
