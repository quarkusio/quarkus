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

import javax.crypto.SecretKey;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jose4j.jwx.HeaderParameterNames;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TrustStoreUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(CustomOidcWiremockTestResource.class)
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
    public void testTenantIdFromRoutingContextDefaultTenantResolver() {
        String username = "alice";
        String tenantId = "bearer";
        String accessToken = getAccessToken(username, Set.of("user"));

        RestAssured.given().auth().oauth2(accessToken)
                .queryParam("includeTenantId", Boolean.TRUE)
                .when().get("/api/users/preferredUserName/bearer")
                .then()
                .statusCode(200)
                .body("userName", equalTo(username))
                .body("tenantId", equalTo(tenantId));

        RestAssured.given().auth().oauth2(getAccessToken(username, Set.of("user", "admin")))
                .when().get("/api/users/preferredUserName/bearer/token")
                .then()
                .statusCode(200)
                .body("userName", equalTo(username))
                .body("tenantId", equalTo(tenantId));
    }

    @Test
    public void testAccessResourceAzure() throws Exception {
        String azureToken = readFile("token.txt");
        String azureJwk = readFile("jwks.json");
        wireMockServer.stubFor(WireMock.get("/auth/azure/jwk")
                .withHeader("Authorization", matching("Access token: " + azureToken))
                .withHeader("Filter", matching("OK"))
                .withHeader("tenant-id", matching("bearer-azure"))
                .willReturn(WireMock.aResponse().withBody(azureJwk)));
        RestAssured.given().auth().oauth2(azureToken)
                .when().get("/api/admin/bearer-azure")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(
                        "Name:jondoe@quarkusoidctest.onmicrosoft.com,Issuer:https://sts.windows.net/e7861267-92c5-4a03-bdb2-2d3e491e7831/"));

        String accessTokenWithCert = TestUtils.createTokenWithInlinedCertChain("alice-certificate");

        RestAssured.given().auth().oauth2(accessTokenWithCert)
                .when().get("/api/admin/bearer-azure")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Name:alice-certificate,Issuer:https://server.example.com"));
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
    public void testBearerTokenEncryptedWithPublicKey() {
        // We can pass encrypted ID token as if it were an encrypted access token
        String encryptedToken = OidcWiremockTestResource.getEncryptedIdToken("admin", Set.of("admin"));
        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-without-decryption-key")
                .then()
                .statusCode(401);

        // This endpoint expects that a token was encrypted with the client secret key
        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-with-client-secret")
                .then()
                .statusCode(401);

        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-with-decryption-key")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    @Test
    public void testBearerTokenEncryptedWithClientSecret() throws Exception {
        // We can pass encrypted ID token as if it were an encrypted access token

        SecretKey encryptionKey = OidcUtils.createSecretKeyFromDigest(
                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");
        String token = OidcWiremockTestResource.getIdToken("admin", Set.of("admin"));
        String encryptedToken = OidcUtils.encryptString(token, encryptionKey);

        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-without-decryption-key")
                .then()
                .statusCode(401);

        // This endpoint expects that a token was encrypted with the public key
        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-with-decryption-key")
                .then()
                .statusCode(401);

        RestAssured.given().auth().oauth2(encryptedToken)
                .when().get("/api/admin/bearer-encrypted-with-client-secret")
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
    public void testCertChainWithCustomValidator() throws Exception {
        List<X509Certificate> chain = TestUtils.loadCertificateChain();
        PrivateKey subjectPrivateKey = TestUtils.loadLeafCertificatePrivateKey();

        // Send the token with the valid certificate chain and bind it to the token claim
        String accessToken = getAccessTokenForCustomValidator(
                chain,
                subjectPrivateKey, "https://service.example.com", true, false);

        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-chain-custom-validator")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // Send the token with the valid certificate chain but do not bind it to the token claim
        accessToken = getAccessTokenForCustomValidator(
                chain,
                subjectPrivateKey, "https://service.example.com", false, false);

        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-chain-custom-validator")
                .then()
                .statusCode(401);

        // Send the token with the valid certificate chain bound to the token claim, but expired
        accessToken = getAccessTokenForCustomValidator(
                chain,
                subjectPrivateKey, "https://service.example.com", true, true);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-chain-custom-validator")
                .then()
                .statusCode(401);

        // Send the token with the valid certificate chain but with the wrong audience
        accessToken = getAccessTokenForCustomValidator(
                chain,
                subjectPrivateKey, "https://server.example.com", true, false);

        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-chain-custom-validator")
                .then()
                .statusCode(401);

    }

    @Test
    public void testAccessAdminResourceWithFullCertChain() throws Exception {
        // index 2 - root, index 1 - intermediate, index 0 - leaf
        List<X509Certificate> chain = TestUtils.loadCertificateChain();
        PrivateKey subjectPrivateKey = TestUtils.loadLeafCertificatePrivateKey();

        // Send the token with the valid certificate chain and bind it to the token claim
        String accessToken = getAccessTokenWithCertChain(
                chain,
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
                chain,
                KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate());
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but which are in the wrong order in the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(1), chain.get(0), chain.get(2)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but with the intermediate one omitted from the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(0), chain.get(2)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

        // Send the token with the only the last valid certificate
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(0)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain")
                .then()
                .statusCode(401);

    }

    @Test
    public void testFullCertChainWithOnlyRootInTruststore() throws Exception {
        List<X509Certificate> chain = TestUtils.loadCertificateChain();
        PrivateKey subjectPrivateKey = TestUtils.loadLeafCertificatePrivateKey();

        // Send the token with the valid certificate chain
        String accessToken = getAccessTokenWithCertChain(
                chain,
                subjectPrivateKey);

        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain-root-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));

        // Send the same token to the service expecting a different leaf certificate name
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain-root-only-wrongcname")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but which are in the wrong order in the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(1), chain.get(0), chain.get(2)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain-root-only")
                .then()
                .statusCode(401);

        // Send the token with the valid certificates but with the intermediate one omitted from the chain
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(0), chain.get(2)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain-root-only")
                .then()
                .statusCode(401);

        // Send the token with the only the last valid certificate
        accessToken = getAccessTokenWithCertChain(
                List.of(chain.get(0)),
                subjectPrivateKey);
        RestAssured.given().auth().oauth2(accessToken)
                .when().get("/api/admin/bearer-certificate-full-chain-root-only")
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

        List<X509Certificate> chain = TestUtils.loadCertificateChain();
        PrivateKey subjectPrivateKey = TestUtils.loadLeafCertificatePrivateKey();

        // Send the token with the valid certificate chain
        token = getAccessTokenWithCertChain(
                chain,
                subjectPrivateKey);

        TestUtils.assertX5cOnlyIsPresent(token);

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
                List.of(chain.get(1), chain.get(0), chain.get(2)),
                subjectPrivateKey);

        TestUtils.assertX5cOnlyIsPresent(token);

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
                .jws().keyId("1").chain(List.of(chain.get(1), chain.get(0), chain.get(2)))
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

    // point of this test method mainly to test native mode
    @Test
    public void testJwtClaimPermissionChecker() {
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin"), SignatureAlgorithm.PS256))
                .when().get("/api/admin/bearer-permission-checker")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
        // permission checker deny access as query param signals "fail"
        RestAssured.given().auth().oauth2(getAccessToken("admin", Set.of("admin"), SignatureAlgorithm.PS256))
                .queryParam("fail", "true")
                .when().get("/api/admin/bearer-permission-checker")
                .then()
                .statusCode(403);
        // permission checker deny access as preferred name is 'other-admin' and not 'admin'
        RestAssured.given().auth().oauth2(getAccessToken("other-admin", Set.of("admin"), SignatureAlgorithm.PS256))
                .when().get("/api/admin/bearer-permission-checker")
                .then()
                .statusCode(403);
    }

    @Test
    public void testMultipleRequiredClaimValues() {
        // required claim values "one", "two", and "three" are missing
        RestAssured.given().auth().oauth2(getAccessToken(null))
                .when().get("/api/admin/bearer-required-claims")
                .then()
                .statusCode(401);
        // required claim values "one" and "two" is missing
        RestAssured.given().auth().oauth2(getAccessToken(Set.of("three")))
                .when().get("/api/admin/bearer-required-claims")
                .then()
                .statusCode(401);
        // required claim value "two" is missing
        RestAssured.given().auth().oauth2(getAccessToken(Set.of("one", "three")))
                .when().get("/api/admin/bearer-required-claims")
                .then()
                .statusCode(401);
        // all required claim values are there
        RestAssured.given().auth().oauth2(getAccessToken(Set.of("one", "two", "three")))
                .when().get("/api/admin/bearer-required-claims")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("admin"));
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return getAccessToken(userName, groups, SignatureAlgorithm.RS256);
    }

    private String getAccessToken(Set<String> claimValues) {
        var jwtBuilder = Jwt.preferredUserName("admin")
                .groups(Set.of("admin"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com");
        if (claimValues != null) {
            jwtBuilder.claim("my-claim", claimValues);
        }
        return jwtBuilder
                .jws().algorithm(SignatureAlgorithm.PS256)
                .sign();
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
                .claim("root-certificate-thumbprint", TrustStoreUtils.calculateThumprint(chain.get(chain.size() - 1)))
                .jws()
                .chain(chain)
                .sign(privateKey);
    }

    private String getAccessTokenForCustomValidator(List<X509Certificate> chain,
            PrivateKey privateKey, String aud, boolean setLeafCertThumbprint, boolean expired) throws Exception {
        JwtClaimsBuilder builder = Jwt.preferredUserName("alice")
                .groups("admin")
                .issuer("https://server.example.com")
                .audience(aud)
                .claim("root-certificate-thumbprint", TrustStoreUtils.calculateThumprint(chain.get(chain.size() - 1)));
        if (setLeafCertThumbprint) {
            builder.claim("leaf-certificate-thumbprint", TrustStoreUtils.calculateThumprint(chain.get(0)));
        }
        if (expired) {
            builder.expiresIn(1);
        }
        String jwt = builder.jws()
                .chain(chain)
                .sign(privateKey);
        if (expired) {
            Thread.sleep(2000);
        }
        return jwt;
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
