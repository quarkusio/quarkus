package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jose4j.jwx.HeaderParameterNames;
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
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.vertx.core.json.JsonObject;

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
        String azureToken = readFile("token.txt");
        String azureJwk = readFile("jwks.json");
        wireMockServer.stubFor(WireMock.get("/auth/azure/jwk")
                .withHeader("Authorization", matching("Access token: " + azureToken))
                .willReturn(WireMock.aResponse().withBody(azureJwk)));
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
    public void testAccessAdminResourceWithWrongCertThumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithWrongThumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-no-introspection")
                .then()
                .statusCode(401);
    }

    @Test
    public void testAccessAdminResourceWithCertS256Thumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithS256Thumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-no-introspection")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testAccessAdminResourceWithWrongCertS256Thumbprint() {
        RestAssured.given().auth().oauth2(getAccessTokenWithWrongS256Thumbprint("admin", Set.of("admin")))
                .when().get("/api/admin/bearer-no-introspection")
                .then()
                .statusCode(401);
    }

    @Test
    public void testAccessAdminResourceWithFullCertChain() throws Exception {
        X509Certificate rootCert = KeyUtils.getCertificate(ResourceUtils.readResource("/ca.cert.pem"));
        X509Certificate intermediateCert = KeyUtils.getCertificate(ResourceUtils.readResource("/intermediate.cert.pem"));
        X509Certificate subjectCert = KeyUtils.getCertificate(ResourceUtils.readResource("/www.quarkustest.com.cert.pem"));
        PrivateKey subjectPrivateKey = KeyUtils.readPrivateKey("/www.quarkustest.com.key.pem");
        // Send the token with the valid certificate chain
        String accessToken = getAccessTokenWithCertChain(
                List.of(subjectCert, intermediateCert, rootCert),
                subjectPrivateKey);

        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // send the same token to the endpoint which does not allow a fallback to x5c
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(401);

        // Send the token with the valid certificate chain, but with the token signed by a non-matching private key
        accessToken = getAccessTokenWithCertChain(
                List.of(subjectCert, intermediateCert, rootCert),
                KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate());
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but which are are in the wrong order in the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(intermediateCert, subjectCert, rootCert),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but with the intermediate one omitted from the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(subjectCert, rootCert),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the only the last valid certificate
        accessToken = getAccessTokenWithCertChain(
                List.of(subjectCert),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

    }

    @Test
    public void testAccessAdminResourceWithKidOrChain() throws Exception {
        // token with a matching kid, not x5c
        String token = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();

        assertKidOnlyIsPresent(token, "1");

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // token without kid and x5c
        token = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate());
        assertNoKidAndX5cArePresent(token);

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(401);

        // token with a kid which will resolve to a non-matching public key, no x5c
        token = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().keyId("1")
                .sign(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate());

        assertKidOnlyIsPresent(token, "1");

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(401);

        X509Certificate rootCert = KeyUtils.getCertificate(ResourceUtils.readResource("/ca.cert.pem"));
        X509Certificate intermediateCert = KeyUtils.getCertificate(ResourceUtils.readResource("/intermediate.cert.pem"));
        X509Certificate subjectCert = KeyUtils.getCertificate(ResourceUtils.readResource("/www.quarkustest.com.cert.pem"));
        PrivateKey subjectPrivateKey = KeyUtils.readPrivateKey("/www.quarkustest.com.key.pem");

        // Send the token with the valid certificate chain
        token = getAccessTokenWithCertChain(
                List.of(subjectCert, intermediateCert, rootCert),
                subjectPrivateKey);

        assertX5cOnlyIsPresent(token);

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // send the same token to the endpoint which does not allow a fallback to x5c
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer")
                .then()
                .statusCode(401);

        // Send the token with the valid certificate chain with certificates in the wrong order
        token = getAccessTokenWithCertChain(
                List.of(intermediateCert, subjectCert, rootCert),
                subjectPrivateKey);

        assertX5cOnlyIsPresent(token);

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(401);

        // Send token signed by the subject private key but with a kid which will resolve to
        // a non-matching public key and x5c
        token = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().keyId("1").chain(List.of(intermediateCert, subjectCert, rootCert))
                .sign(subjectPrivateKey);

        assertBothKidAndX5cArePresent(token, "1");

        RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(401);

        // no token
        RestAssured.when().get("/api/admin/bearer-kid-or-chain")
                .then()
                .statusCode(401);
    }

    private void assertNoKidAndX5cArePresent(String token) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        assertFalse(headers.containsKey("x5c"));
        assertFalse(headers.containsKey("kid"));
        assertFalse(headers.containsKey("x5t"));
        assertFalse(headers.containsKey("x5t#S256"));
    }

    private void assertBothKidAndX5cArePresent(String token, String kid) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        assertTrue(headers.containsKey("x5c"));
        assertEquals(kid, headers.getString("kid"));
        assertFalse(headers.containsKey("x5t"));
        assertFalse(headers.containsKey("x5t#S256"));
    }

    private void assertKidOnlyIsPresent(String token, String kid) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        assertFalse(headers.containsKey("x5c"));
        assertEquals(kid, headers.getString("kid"));
        assertFalse(headers.containsKey("x5t"));
        assertFalse(headers.containsKey("x5t#S256"));
    }

    private void assertX5cOnlyIsPresent(String token) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        assertTrue(headers.containsKey("x5c"));
        assertFalse(headers.containsKey("kid"));
        assertFalse(headers.containsKey("x5t"));
        assertFalse(headers.containsKey("x5t#S256"));
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
    public void testBearerToken() {
        String token = getAccessToken("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"));
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
    public void testBearerTokenIdScheme() {
        String token = getAccessToken("alice", Set.of("user"));

        RestAssured.given().header("Authorization", "ID " + token).when()
                .get("/api/users/me/bearer-id")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"));
    }

    @Test
    public void testBearerTokenIdSchemeButBearerSchemeIsUsed() {
        String token = getAccessToken("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer-id")
                .then()
                .statusCode(401);
    }

    @Test
    public void testBearerTokenIdSchemeWrongIssuer() {
        String token = getAccessTokenWrongIssuer("alice", Set.of("user"));

        RestAssured.given().auth().oauth2(token).when()
                .get("/api/users/me/bearer-id")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("ID"));
    }

    @Test
    public void testAcquiringIdentityOutsideOfHttpRequest() {
        String tenant = "bearer";
        String user = "alice";
        String role = "user";
        assertSecurityIdentityAcquired(tenant, user, role);

        tenant = "bearer-role-claim-path";
        user = "admin";
        role = "admin";
        assertSecurityIdentityAcquired(tenant, user, role);

        // test with event bus
        RestAssured.given().auth().oauth2(getAccessToken("alice", Set.of("customer"))).body("ProductXYZ").post("order/bearer")
                .then().statusCode(204);
        Awaitility
                .await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .untilAsserted(() -> RestAssured.given().get("order/bearer").then().statusCode(200).body(Matchers.is("alice")));
    }

    @Test
    public void testGrpcAuthorizationWithBearerToken() {
        String token = getAccessToken("alice", Set.of("user"));
        RestAssured.given().auth().oauth2(token).when()
                .get("/api/greeter/bearer")
                .then()
                .statusCode(500);

        token = getAccessToken("alice", Set.of("admin"));
        RestAssured.given().auth().oauth2(token).when()
                .get("/api/greeter/bearer")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Hello Jonathan from alice"));
    }

    private static void assertSecurityIdentityAcquired(String tenant, String user, String role) {
        String jsonPath = tenant + "." + user + ".findAll{ it == \"" + role + "\"}.size()";
        RestAssured.given().when().get("/startup-service").then().statusCode(200)
                .body(jsonPath, Matchers.is(1));
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

    private String getAccessTokenWithWrongThumbprint(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().header(HeaderParameterNames.X509_CERTIFICATE_THUMBPRINT, "123")
                .sign("privateKeyWithoutKid.jwk");
    }

    private String getAccessTokenWithS256Thumbprint(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().thumbprintS256(OidcWiremockTestResource.getCertificate())
                .sign("privateKeyWithoutKid.jwk");
    }

    private String getAccessTokenWithWrongS256Thumbprint(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().header(HeaderParameterNames.X509_CERTIFICATE_SHA256_THUMBPRINT, "123")
                .sign("privateKeyWithoutKid.jwk");
    }

    private String getAccessTokenWithCertChain(List<X509Certificate> chain,
            PrivateKey privateKey) throws Exception {
        return Jwt.preferredUserName("alice")
                .groups("admin")
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().chain(chain)
                .sign(privateKey);
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
