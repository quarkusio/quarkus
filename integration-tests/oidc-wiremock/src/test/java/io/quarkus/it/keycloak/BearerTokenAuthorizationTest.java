package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.hamcrest.Matchers.equalTo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
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
    public void testAccessResourceAzure() throws Exception {
        String azureJwk = readFile("jwks.json");
        wireMockServer.stubFor(WireMock.get("/auth/azure/jwk")
                .willReturn(WireMock.aResponse().withBody(azureJwk)));
        String azureToken = readFile("token.txt");
        RestAssured.given().auth().oauth2(azureToken)
                .when().get("/api/admin/bearer-azure")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Issuer:https://sts.windows.net/e7861267-92c5-4a03-bdb2-2d3e491e7831/"));
    }

    private String readFile(String filePath) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            byte[] content = FileUtil.readFileContents(is);
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testAccessAdminResource() {
        String token = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().header("customize", true)
                .sign();

        // 1st pass with `RS256` - OK
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // 2nd pass with `RS256` - 401 because the customizer removes `customize` header
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(401);

        // replacing RS256 with RS384 fails - the customizer does nothing
        token = setTokenAlgorithm(token, "RS384");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(401);

        // replacing RS256 with RS512 - OK, customizer sets the algorithm to the original RS256
        token = setTokenAlgorithm(token, "RS512");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    private static String setTokenAlgorithm(String token, String alg) {
        io.vertx.core.json.JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        headers.put("alg", alg);
        String newHeaders = new String(
                Base64.getUrlEncoder().withoutPadding().encode(headers.toString().getBytes()),
                StandardCharsets.UTF_8);
        int dotIndex = token.indexOf('.');
        return newHeaders + token.substring(dotIndex);
    }

    @Test
    public void testAccessAdminResourceRequiredAlgorithm() {
        // RS256 is rejected
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-required-algorithm")
                .then()
                .statusCode(401);
        // PS256 is OK
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin"), SignatureAlgorithm.PS256))
                .when().get("/api/admin/bearer-required-algorithm")
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
    public void testAccessAdminResourceWithCustomRolePathForbidden() {
        RestAssured.given().auth().oauth2(getAccessTokenWithCustomRolePath("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-role-claim-path")
                .then()
                .statusCode(403);
    }

    @Test
    public void testAccessAdminResourceWithCustomRolePath() {
        RestAssured.given().auth().oauth2(getAccessTokenWithCustomRolePath("admin", Set.of("custom")))
                .when().get("/api/admin/bearer-role-claim-path")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("custom"));
    }

    @Test
    public void testAccessAdminResourceWithoutKidAndThumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithoutKidAndThumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-no-introspection")
                .then()
                .statusCode(401);
    }

    @Test
    public void testTokenAndKeyWithoutKidAndThumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithoutKidAndThumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-key-without-kid-thumbprint")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testTokenWithKidAndKeyWithoutKidAndThumbprint() {
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-key-without-kid-thumbprint")
                .then()
                .statusCode(401);
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
        return getAccessToken(userName, groups, SignatureAlgorithm.RS256);
    }

    private String getAccessToken(String userName, Set<String> groups, SignatureAlgorithm alg) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().algorithm(alg)
                .sign();
    }

    private String getAccessTokenWithCustomRolePath(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .claim("https://roles.example.com", groups)
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

    private String getAccessTokenWithoutKidAndThumbprint(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
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
