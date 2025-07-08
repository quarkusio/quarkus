package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.awaitility.core.ThrowingRunnable;
import org.eclipse.microprofile.jwt.Claims;
import org.hamcrest.Matchers;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class CodeFlowAuthorizationTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @BeforeAll
    public static void clearCache() {
        // clear token cache to make tests idempotent as we experienced failures
        // on Windows when BearerTokenAuthorizationTest run before CodeFlowAuthorizationTest
        RestAssured
                .given()
                .get("http://localhost:8081/clear-token-cache")
                .then()
                .statusCode(204);
    }

    @Test
    public void testCodeFlow() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice, cache size: 0", textPage.getContent());
            assertNotNull(getSessionCookie(webClient, "code-flow"));
            // Logout
            textPage = webClient.getPage("http://localhost:8081/code-flow/logout");
            assertEquals("Welcome, clientId: quarkus-web-app", textPage.getContent());
            assertNull(getSessionCookie(webClient, "code-flow"));
            // Clear the post logout cookie
            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowOpaqueAccessToken() throws IOException {
        defineCodeFlowOpaqueAccessTokenStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-opaque-access-token");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice", textPage.getContent());

            try {
                webClient.getPage("http://localhost:8081/code-flow-opaque-access-token/jwt-access-token");
                fail("500 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(500, ex.getStatusCode());
            }

            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowVerifyIdAndAccessToken() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-verify-id-and-access-tokens");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("access token verified: true,"
                    + " id_token issuer: https://server.example.com,"
                    + " access_token issuer: https://server.example.com,"
                    + " id_token audience: https://id.server.example.com;quarkus-web-app,"
                    + " access_token audience: https://server.example.com,"
                    + " cache size: 0", textPage.getContent());
            assertNotNull(getSessionCookie(webClient, "code-flow-verify-id-and-access-tokens"));
            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowEncryptedIdTokenJwk() throws IOException {
        doTestCodeFlowEncryptedIdToken("code-flow-encrypted-id-token-jwk", KeyEncryptionAlgorithm.DIR);
    }

    @Test
    public void testCodeFlowEncryptedIdTokenPem() throws IOException {
        doTestCodeFlowEncryptedIdToken("code-flow-encrypted-id-token-pem", KeyEncryptionAlgorithm.A256GCMKW);
    }

    private void doTestCodeFlowEncryptedIdToken(String tenant, KeyEncryptionAlgorithm alg) throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-encrypted-id-token/" + tenant);

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("user: alice", textPage.getContent());
            Cookie sessionCookie = getSessionCookie(webClient, tenant);
            assertNotNull(sessionCookie);
            // All the session cookie content is encrypted
            String[] sessionCookieParts = sessionCookie.getValue().split("\\|");
            assertEquals(1, sessionCookieParts.length);
            assertTrue(isEncryptedToken(sessionCookieParts[0], alg));
            JsonObject headers = OidcUtils.decodeJwtHeaders(sessionCookieParts[0]);
            assertEquals(alg.getAlgorithm(), headers.getString("alg"));

            // repeat the call with the session cookie containing the encrypted id token
            textPage = webClient.getPage("http://localhost:8081/code-flow-encrypted-id-token/" + tenant);
            assertEquals("user: alice", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowEncryptedIdTokenDisabled() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/code-flow-encrypted-id-token/code-flow-encrypted-id-token-disabled");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            try {
                form.getInputByValue("login").click();
                fail("ID token decryption is disabled");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getResponse().getStatusCode());
            }

            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    private static boolean isEncryptedToken(String token, KeyEncryptionAlgorithm alg) {
        int expectedNonEmptyParts = alg == KeyEncryptionAlgorithm.DIR ? 4 : 5;
        return new StringTokenizer(token, ".").countTokens() == expectedNonEmptyParts;
    }

    @Test
    public void testCodeFlowFormPostAndBackChannelLogout() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-form-post");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice", textPage.getContent());

            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            textPage = webClient.getPage("http://localhost:8081/code-flow-form-post");
            assertEquals("alice", textPage.getContent());

            // Session is still active
            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            // ID token subject is `123456`
            // request a back channel logout for some other subject
            RestAssured.given()
                    .when().contentType(ContentType.URLENC)
                    .body("logout_token=" + OidcWiremockTestResource.getLogoutToken("789"))
                    .post("/back-channel-logout")
                    .then()
                    .statusCode(200);

            // No logout:
            textPage = webClient.getPage("http://localhost:8081/code-flow-form-post");
            assertEquals("alice", textPage.getContent());
            // Session is still active
            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            // request a back channel logout for the same subject
            RestAssured.given()
                    .when().contentType(ContentType.URLENC).body("logout_token="
                            + OidcWiremockTestResource.getLogoutToken("123456"))
                    .post("/back-channel-logout")
                    .then()
                    .statusCode(200);

            // Confirm 302 is returned and the session cookie is null
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/code-flow-form-post").toURL()));
            assertEquals(302, webResponse.getStatusCode());

            assertNull(getSessionCookie(webClient, "code-flow-form-post"));

            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowFormPostAndFrontChannelLogout() throws Exception {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-form-post");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice", textPage.getContent());

            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            textPage = webClient.getPage("http://localhost:8081/code-flow-form-post");
            assertEquals("alice", textPage.getContent());

            // Session is still active
            JsonObject idTokenClaims = decryptIdToken(webClient, "code-flow-form-post");

            webClient.getOptions().setRedirectEnabled(false);

            // Confirm 302 is returned and the session cookie is null when the frontchannel logout URL is called
            URL frontchannelUrl = URI.create("http://localhost:8081/code-flow-form-post/front-channel-logout"
                    + "?sid=" + idTokenClaims.getString("sid") + "&iss="
                    + OidcCommonUtils.urlEncode(idTokenClaims.getString("iss"))).toURL();
            WebResponse webResponse = webClient.loadWebResponse(new WebRequest(frontchannelUrl));
            assertEquals(302, webResponse.getStatusCode());

            assertNull(getSessionCookie(webClient, "code-flow-form-post"));

            // remove the state cookie for Quarkus not to treat the next call as an expected redirect from OIDC
            webClient.getCookieManager().clearCookies();

            // Confirm 302 is returned and the session cookie is null when the endpoint is called
            webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/code-flow-form-post").toURL()));
            assertEquals(302, webResponse.getStatusCode());

            assertNull(getSessionCookie(webClient, "code-flow-form-post"));

            webClient.getCookieManager().clearCookies();
        }
        clearCache();
    }

    @Test
    public void testCodeFlowUserInfo() throws Exception {
        defineCodeFlowAuthorizationOauth2TokenStub();
        wireMockServer.resetRequests();
        // No internal ID token
        doTestCodeFlowUserInfo("code-flow-user-info-only", 300, false, false, 1, 1);
        clearCache();
        // Internal ID token, allow in memory cache = true, cacheUserInfoInIdtoken = false without having to be configured
        doTestCodeFlowUserInfo("code-flow-user-info-github", 25200, false, false, 1, 1);
        clearCache();
        // Internal ID token, allow in memory cache = false, cacheUserInfoInIdtoken = true without having to be configured
        doTestCodeFlowUserInfo("code-flow-user-info-dynamic-github", 301, true, true, 0, 1);
        clearCache();
        // Internal ID token, allow in memory cache = false, cacheUserInfoInIdtoken = false
        doTestCodeFlowUserInfo("code-flow-user-info-github-cache-disabled", 25200, false, false, 0, 4);
        clearCache();
        doTestCodeFlowUserInfoDynamicGithubUpdate();
        clearCache();
    }

    @Test
    public void testCodeFlowUserInfoCachedInIdToken() throws Exception {
        // Internal ID token, allow in memory cache = false, cacheUserInfoInIdtoken = true
        final String refreshJwtToken = generateAlreadyExpiredRefreshToken();
        defineCodeFlowUserInfoCachedInIdTokenStub(refreshJwtToken);
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            Cookie stateCookie = getStateCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            Date stateCookieDate = stateCookie.getExpires();
            final long nowInSecs = nowInSecs();
            final long sessionCookieLifespan = stateCookieDate.toInstant().getEpochSecond() - nowInSecs;
            // 5 mins is default
            assertTrue(sessionCookieLifespan >= 299 && sessionCookieLifespan <= 304);

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice:alice:alice, cache size: 0, TenantConfigResolver: false, refresh_token:refresh1234",
                    textPage.getContent());

            assertNull(getStateCookie(webClient, "code-flow-user-info-github-cached-in-idtoken"));

            JsonObject idTokenClaims = decryptIdToken(webClient, "code-flow-user-info-github-cached-in-idtoken");
            assertNotNull(idTokenClaims.getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE));

            long issuedAt = idTokenClaims.getLong("iat");
            long expiresAt = idTokenClaims.getLong("exp");
            assertEquals(299, expiresAt - issuedAt);

            Cookie sessionCookie = getSessionCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            Date date = sessionCookie.getExpires();
            assertTrue(date.toInstant().getEpochSecond() - issuedAt >= 299 + 300);
            // This test enables the token refresh, in this case the cookie age is extended by additional 5 mins
            // to minimize the risk of the browser losing immediately after it has expired, for this cookie
            // be returned to Quarkus, analyzed and refreshed
            assertTrue(date.toInstant().getEpochSecond() - issuedAt <= 299 + 300 + 3);

            // This is the initial call to  the token endpoint where the code was exchanged for tokens
            wireMockServer.verify(1,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/access_token_refreshed")));
            wireMockServer.resetRequests();

            // refresh: refresh token in JWT format
            Thread.sleep(3000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken");
            assertEquals("alice:alice:bob, cache size: 0, TenantConfigResolver: false, refresh_token:" + refreshJwtToken,
                    textPage.getContent());

            idTokenClaims = decryptIdToken(webClient, "code-flow-user-info-github-cached-in-idtoken");
            assertNotNull(idTokenClaims.getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE));

            issuedAt = idTokenClaims.getLong("iat");
            expiresAt = idTokenClaims.getLong("exp");
            assertEquals(305, expiresAt - issuedAt);

            sessionCookie = getSessionCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            date = sessionCookie.getExpires();
            assertTrue(date.toInstant().getEpochSecond() - issuedAt >= 305 + 300);
            assertTrue(date.toInstant().getEpochSecond() - issuedAt <= 305 + 300 + 3);

            // access token must've been refreshed
            wireMockServer.verify(1,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/access_token_refreshed")));
            wireMockServer.resetRequests();

            Thread.sleep(3000);
            // Refresh token is available but it is expired, so no token endpoint call is expected
            assertTrue((System.currentTimeMillis() / 1000) > OidcCommonUtils.decodeJwtContent(refreshJwtToken)
                    .getLong(Claims.exp.name()));

            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(
                            URI.create("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken").toURL()));
            assertEquals(302, webResponse.getStatusCode());

            // no another token endpoint call is made:
            wireMockServer.verify(0,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/access_token_refreshed")));
            wireMockServer.resetRequests();

            webClient.getCookieManager().clearCookies();
        }

        // Now send a bearer access token with the inline chain
        String bearerAccessToken = TestUtils.createTokenWithInlinedCertChain("alice-certificate");

        RestAssured.given().auth().oauth2(bearerAccessToken)
                .when().get("/code-flow-user-info-github-cached-in-idtoken")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("alice:alice:alice-certificate, cache size: 0, TenantConfigResolver: false"));

        clearCache();
        checkSignedUserInfoRecordInLog();
    }

    private void checkSignedUserInfoRecordInLog() {
        final Path logDirectory = Paths.get(".", "target");
        given().await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        boolean fileExists = Files.exists(accessLogFilePath);
                        if (!fileExists) {
                            accessLogFilePath = logDirectory.resolve("target/quarkus.log");
                            fileExists = Files.exists(accessLogFilePath);
                        }
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
                                "quarkus log file " + accessLogFilePath + " is missing");

                        boolean lineConfirmingVerificationDetected = false;
                        boolean signedUserInfoResponseFilterMessageDetected = false;
                        boolean codeFlowCompletedResponseFilterMessageDetected = false;

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("Verifying the signed UserInfo with the local JWK keys: ey")) {
                                    lineConfirmingVerificationDetected = true;
                                } else if (line.contains("Response contains signed UserInfo")) {
                                    signedUserInfoResponseFilterMessageDetected = true;
                                } else if (line.contains(
                                        "Authorization code completed for tenant 'code-flow-user-info-github-cached-in-idtoken' in an instant: true")) {
                                    codeFlowCompletedResponseFilterMessageDetected = true;
                                }
                                if (lineConfirmingVerificationDetected
                                        && signedUserInfoResponseFilterMessageDetected
                                        && codeFlowCompletedResponseFilterMessageDetected) {
                                    break;
                                }
                            }

                        }
                        assertTrue(lineConfirmingVerificationDetected,
                                "Log file must contain a record confirming that signed UserInfo is verified");
                        assertTrue(signedUserInfoResponseFilterMessageDetected,
                                "Log file must contain a record confirming that signed UserInfo is returned");
                        assertTrue(codeFlowCompletedResponseFilterMessageDetected,
                                "Log file must contain a record confirming that the code flow is completed");

                    }
                });
    }

    @Test
    public void testCodeFlowTokenIntrospectionActiveRefresh() throws Exception {
        // This stub does not return an access token expires_in property
        defineCodeFlowTokenIntrospectionStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-token-introspection");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice:alice", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection");
            assertEquals("alice:alice", textPage.getContent());

            // Refresh
            // The internal ID token lifespan is 5 mins
            // Configured refresh token skew is 298 secs = 5 mins - 2 secs
            // Therefore, after waiting for 3 secs, an active refresh is happening
            Thread.sleep(3000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection");
            assertEquals("admin:admin", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }

        clearCache();
    }

    @Test
    public void testCodeFlowTokenIntrospectionActiveRefresh_noEncryption() throws Exception {
        // exactly as testCodeFlowTokenIntrospectionActiveRefresh but with
        // quarkus.oidc.token-state-manager.split-tokens=true
        // quarkus.oidc.token-state-manager.encryption-required=false
        // to assure that cookie is valid when there are multiple scopes

        // This stub does not return an access token expires_in property
        defineCodeFlowTokenIntrospectionStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-token-introspection-no-encryption");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice:alice", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection-no-encryption");
            assertEquals("alice:alice", textPage.getContent());

            // Refresh
            // The internal ID token lifespan is 5 mins
            // Configured refresh token skew is 298 secs = 5 mins - 2 secs
            // Therefore, after waiting for 3 secs, an active refresh is happening
            Thread.sleep(3000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection-no-encryption");
            assertEquals("admin:admin", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }

        clearCache();
    }

    @Test
    public void testCodeFlowTokenIntrospectionExpiresInRefresh() throws Exception {
        // This stub does return an access token expires_in property
        defineCodeFlowTokenIntrospectionExpiresInStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-token-introspection-expires-in");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice", textPage.getContent());

            // Refresh the expired token
            // The internal ID token lifespan is 5 mins, refresh token skew is not configured,
            // code flow access token expires in 3 seconds from now. Therefore, after waiting for 5 secs
            // the refresh is triggered because it is allowed in the config and token expires_in property is returned.
            Thread.sleep(5000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection-expires-in");
            assertEquals("bob", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }

        clearCache();
    }

    private void doTestCodeFlowUserInfo(String tenantId, long internalIdTokenLifetime, boolean cacheUserInfoInIdToken,
            boolean tenantConfigResolver, int inMemoryCacheSize, int userInfoRequests) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            wireMockServer.verify(0, getRequestedFor(urlPathMatching("/auth/realms/quarkus/protocol/openid-connect/userinfo")));
            HtmlPage page = webClient.getPage("http://localhost:8081/" + tenantId);

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals(
                    "alice:alice:alice, cache size: " + inMemoryCacheSize + ", TenantConfigResolver: " + tenantConfigResolver,
                    textPage.getContent());
            textPage = webClient.getPage("http://localhost:8081/" + tenantId);
            assertEquals(
                    "alice:alice:alice, cache size: " + inMemoryCacheSize + ", TenantConfigResolver: " + tenantConfigResolver,
                    textPage.getContent());
            textPage = webClient.getPage("http://localhost:8081/" + tenantId);
            assertEquals(
                    "alice:alice:alice, cache size: " + inMemoryCacheSize + ", TenantConfigResolver: " + tenantConfigResolver,
                    textPage.getContent());

            wireMockServer.verify(userInfoRequests,
                    getRequestedFor(urlPathMatching("/auth/realms/quarkus/protocol/openid-connect/userinfo")));
            wireMockServer.resetRequests();

            JsonObject idTokenClaims = decryptIdToken(webClient, tenantId);
            assertEquals(cacheUserInfoInIdToken, idTokenClaims.containsKey(OidcUtils.USER_INFO_ATTRIBUTE));
            long issuedAt = idTokenClaims.getLong("iat");
            long expiresAt = idTokenClaims.getLong("exp");
            assertEquals(internalIdTokenLifetime, expiresAt - issuedAt);

            Cookie sessionCookie = getSessionCookie(webClient, tenantId);
            Date date = sessionCookie.getExpires();
            assertTrue(date.toInstant().getEpochSecond() - issuedAt >= internalIdTokenLifetime);
            assertTrue(date.toInstant().getEpochSecond() - issuedAt <= internalIdTokenLifetime + 3);

            webClient.getCookieManager().clearCookies();
            wireMockServer.resetRequests();
        }
    }

    private void doTestCodeFlowUserInfoDynamicGithubUpdate() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/code-flow-user-info-dynamic-github");

            HtmlForm htmlForm = htmlPage.getFormByName("form");
            htmlForm.getInputByName("username").type("alice");
            htmlForm.getInputByName("password").type("alice");

            TextPage textPage = htmlForm.getInputByValue("login").click();
            assertEquals("alice:alice:alice, cache size: 0, TenantConfigResolver: true", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/code-flow-user-info-dynamic-github");
            assertEquals("alice:alice:alice, cache size: 0, TenantConfigResolver: true", textPage.getContent());

            // Dynamic `code-flow-user-info-dynamic-github` tenant, resource is `code-flow-user-info-dynamic-github`
            checkResourceMetadata("code-flow-user-info-dynamic-github", "quarkus");

            textPage = webClient.getPage("http://localhost:8081/code-flow-user-info-dynamic-github?update=true");
            assertEquals("alice@somecompany.com:alice:alice, cache size: 0, TenantConfigResolver: true", textPage.getContent());

            // Dynamic `code-flow-user-info-dynamic-github` tenant, resource is `github`
            checkResourceMetadata("github", "quarkus");

            htmlPage = webClient.getPage("http://localhost:8081/code-flow-user-info-dynamic-github?reconnect=true");
            htmlForm = htmlPage.getFormByName("form");
            htmlForm.getInputByName("username").type("alice");
            htmlForm.getInputByName("password").type("alice");

            textPage = htmlForm.getInputByValue("login").click();
            assertEquals("alice@anothercompany.com:alice:alice, cache size: 0, TenantConfigResolver: true",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    private JsonObject decryptIdToken(WebClient webClient, String tenantId) throws Exception {
        Cookie sessionCookie = getSessionCookie(webClient, tenantId);
        assertNotNull(sessionCookie);

        SecretKey key = null;
        if ("code-flow-user-info-github".equals(tenantId)) {
            PrivateKey privateKey = KeyUtils.tryAsPemSigningPrivateKey(
                    "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCyXwKqKL/"
                            + "hQWDkurdHyRn/9aZqmrgpCfiT5+gQ7KZ9RvDjgTqkJT6IIrRFvIpeBMwS"
                            + "sw3dkUPGmgN1J4QOhLaR2VEXhc20UbxFbr6HXAskZGPuCL1tzRWDkLNMZaEO8jqhPbcq1Ro4GMhaSdm0sBHmcQnu8wAOrdAowdzGh"
                            + "/HUaaYBDY0OZVAm9N8zzBXTahna9frJCMHq3e9szIiv6HYZTy1672/+hR/0D1HY+bqpQtJnzSrKjkFeXDAbYPgewYLEJ2Dk+oo6L"
                            + "1I6S+UTrl4FRHw1fHAd2i75JD+vL/8w/AtKkej0CCBUSZJiV+KDJWjnDUVRWjq5hQb9pu4qEJKhAgMBAAECggEAJvBs4X7B3MfsAi"
                            + "LszgQN4/3ZlZ4vI+5kUM2osMEo22J4RgI5Lgpfa1LALhUp07qSXmauWTdUJ3AJ3zKANrcsMAzUEiGItZu+UR4LA/vJBunPkvBfgi/"
                            + "qSW12ZvAsx9mDiR2y9evNrH9khalnmHVzgu4ccAimc43oSm1/5+tXlLoZ1QK/FohxBxAshtuDHGs8yKUL0jpv7dOrjhCj2ibmPYe6A"
                            + "Uk9F61sVWO0/i0Q8UAOcYT3L5nCS5WnLhdCdYpIJJ7xl2PrVE/BAD+JEG5uCOYfVeYh+iCZVfpX17ryfNNUaBtyxKEGVtHbje3mO86"
                            + "mYN3noaS0w/zpUjBPgV+KEQKBgQDsp6VTmDIqHFTp2cC2yrDMxRznif92EGv7ccJDZtbTC37mAuf2J7x5b6AiE1EfxEXyGYzSk99sC"
                            + "ns+GbL1EHABUt5pimDCl33b6XvuccQNpnJ0MfM5eRX9Ogyt/OKdDRnQsvrTPNCWOyJjvG01HQM4mfxaBBnxnvl5meH2pyG/ZQKBgQD"
                            + "A87DnyqEFhTDLX5c1TtwHSRj2xeTPGKG0GyxOJXcxR8nhtY9ee0kyLZ14RytnOxKarCFgYXeG4IoGEc/I42WbA4sq88tZcbe4IJkdX"
                            + "0WLMqOTdMrdx9hMU1ytKVUglUJZBVm7FaTQjA+ArMwqkXAA5HBMtArUsfJKUt3l0hMIjQKBgQDS1vmAZJQs2Fj+jzYWpLaneOWrk1K"
                            + "5yR+rQUql6jVyiUdhfS1ULUrJlh3Avh0EhEUc0I6Z/YyMITpztUmu9BoV09K7jMFwHK/RAU+cvFbDIovN4cKkbbCdjt5FFIyBB278d"
                            + "LjrAb+EWOLmoLVbIKICB47AU+8ZSV1SbTrYGUcD0QKBgQCAliZv4na6sg9ZiUPAr+QsKserNSiN5zFkULOPBKLRQbFFbPS1l12pRgL"
                            + "qNCu1qQV19H5tt6arSRpSfy5FB14gFxV4s23yFrnDyF2h2GsFH+MpEq1bbaI1A10AvUnQ5AeKQemRpxPmM2DldMK/H5tPzO0WAOoy4"
                            + "r/ATkc4sG4kxQKBgBL9neT0TmJtxlYGzjNcjdJXs3Q91+nZt3DRMGT9s0917SuP77+FdJYocDiH1rVa9sGG8rkh1jTdqliAxDXwIm5I"
                            + "GS/0OBnkaN1nnGDk5yTiYxOutC5NSj7ecI5Erud8swW6iGqgz2ioFpGxxIYqRlgTv/6mVt41KALfKrYIkVLw",
                    SignatureAlgorithm.RS256);
            key = OidcUtils.createSecretKeyFromDigest(privateKey.getEncoded());
        } else {
            key = OidcUtils.createSecretKeyFromDigest(
                    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
                            .getBytes(StandardCharsets.UTF_8));
        }

        String decryptedSessionCookie = OidcUtils.decryptString(sessionCookie.getValue(), key);

        String encodedIdToken = decryptedSessionCookie.split("\\|")[0];

        return OidcCommonUtils.decodeJwtContent(encodedIdToken);
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private void defineCodeFlowAuthorizationOauth2TokenStub() {
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withHeader("X-Custom", equalTo("XCustomHeaderValue"))
                        .withBasicAuth("quarkus-web-app",
                                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
                        .withRequestBody(containing("extra-param=extra-param-value"))
                        .withRequestBody(containing("authorization_code"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("alice", Set.of()) + "\","
                                        + "  \"refresh_token\": \"refresh1234\""
                                        + "}")));
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withHeader("X-Custom", equalTo("XCustomHeaderValue"))
                        .withRequestBody(containing("extra-param=extra-param-value"))
                        .withRequestBody(containing("authorization_code"))
                        .withRequestBody(
                                containing(
                                        "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"))
                        .withRequestBody(containing("client_assertion=ey"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("alice", Set.of()) + "\","
                                        + "  \"refresh_token\": \"refresh1234\""
                                        + "}")));
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withBasicAuth("quarkus-web-app",
                                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
                        .withRequestBody(containing("refresh_token=refresh1234"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("bob", Set.of()) + "\""
                                        + "}")));

        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/github/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "    \"authorization_endpoint\": \"" + wireMockServer.baseUrl()
                                        + "/auth/realms/quarkus\"," +
                                        "    \"jwks_uri\": \"" + wireMockServer.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/certs\",\n" +
                                        "    \"token_endpoint\": \"" + wireMockServer.baseUrl()
                                        + "/auth/realms/quarkus/token\"," +
                                        "    \"userinfo_endpoint\": \"" + wireMockServer.baseUrl()
                                        + "/auth/realms/github/protocol/openid-connect/userinfo\""
                                        + "}")));
        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/github/protocol/openid-connect/userinfo"))
                        .withHeader("Authorization", containing("Bearer ey"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n"
                                        + "\"preferred_username\": \"alice\","
                                        + "\"personal-email\": \"alice@anothercompany.com\""
                                        + "}")));

    }

    private void defineCodeFlowUserInfoCachedInIdTokenStub(String expiredRefreshToken) {
        wireMockServer
                .stubFor(WireMock.post(urlPathMatching("/auth/realms/quarkus/access_token_refreshed"))
                        .withHeader("X-Custom", matching("XCustomHeaderValue"))
                        .withQueryParam("extra-param", equalTo("extra-param-value"))
                        .withQueryParam("grant_type", equalTo("authorization_code"))
                        .withQueryParam("client_id", equalTo("quarkus-web-app"))
                        .withQueryParam("client_secret", equalTo(
                                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"))
                        .withRequestBody(notContaining("extra-param=extra-param-value"))
                        .withRequestBody(notContaining("authorization_code"))
                        .withRequestBody(notContaining("client_id=quarkus-web-app"))
                        .withRequestBody(notContaining(
                                "client_secret=AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("alice", Set.of()) + "\","
                                        + "\"expires_in\": 299,"
                                        + "  \"refresh_token\": \"refresh1234\""
                                        + "}")));
        wireMockServer
                .stubFor(WireMock.post(urlPathMatching("/auth/realms/quarkus/access_token_refreshed"))
                        .withQueryParam("refresh_token", equalTo("refresh1234"))
                        .withQueryParam("client_id", equalTo("quarkus-web-app"))
                        .withQueryParam("client_secret", equalTo(
                                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"))
                        .withRequestBody(notContaining("refresh_token=refresh1234"))
                        .withRequestBody(notContaining("client_id=quarkus-web-app"))
                        .withRequestBody(notContaining(
                                "client_secret=AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("bob", Set.of()) + "\","
                                        + " \"expires_in\": 305,"
                                        + " \"refresh_token\": \""
                                        + expiredRefreshToken + "\""
                                        + "}")));

        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/protocol/openid-connect/signeduserinfo"))
                        .withHeader("Authorization", containing("Bearer ey"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", " application/jwt ; charset=UTF-8")
                                .withBody(
                                        Jwt.preferredUserName("alice")
                                                .issuer("https://server.example.com")
                                                .audience("quarkus-web-app")
                                                .jws()
                                                .keyId("1").sign("privateKey.jwk"))));

    }

    private void defineCodeFlowOpaqueAccessTokenStub() {
        wireMockServer
                .stubFor(WireMock.post(urlPathMatching("/auth/realms/quarkus/opaque-access-token"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \"alice\","
                                        + "  \"scope\": \"laptop phone\","
                                        + "\"expires_in\": 299}")));
    }

    private String generateAlreadyExpiredRefreshToken() {
        return Jwt.claims().expiresIn(0).signWithSecret("0123456789ABCDEF0123456789ABCDEF");
    }

    private void defineCodeFlowTokenIntrospectionStub() {
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withHeader("X-Custom", matching("XTokenIntrospection"))
                        .withRequestBody(containing("authorization_code"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \"alice\","
                                        + "  \"scope\": \"openid profile email\","
                                        + "  \"refresh_token\": \"refresh5678\""
                                        + "}")));

        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withRequestBody(containing("refresh_token=refresh5678"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \"admin\","
                                        + "  \"scope\": \"openid profile email\""
                                        + "}")));
    }

    private static long nowInSecs() {
        return Instant.now().getEpochSecond();
    }

    private void defineCodeFlowTokenIntrospectionExpiresInStub() {
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token_expires_in")
                        .withRequestBody(containing("authorization_code"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"id_token\": \"" +
                                        OidcWiremockTestResource.generateJwtToken("alice", Set.of(), "sub", "ID",
                                                Set.of("quarkus-web-app"))
                                        + "\","
                                        + "  \"access_token\": \"alice\","
                                        + "  \"expires_in\":" + 3 + ","
                                        + "  \"refresh_token\": \"refresh.expires.in\""
                                        + "}")));

        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/introspect_expires_in")
                        .withRequestBody(containing("token=alice"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n"
                                        + "  \"username\": \"alice\","
                                        + "  \"exp\":" + (nowInSecs() + 3) + ","
                                        + "  \"active\": true"
                                        + "}")));

        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/introspect_expires_in")
                        .withRequestBody(containing("token=bob"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n"
                                        + "  \"username\": \"bob\","
                                        + "  \"active\": true"
                                        + "}")));

        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token_expires_in")
                        .withRequestBody(containing("refresh_token=refresh.expires.in"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \"bob\","
                                        + "  \"id_token\": \"" +
                                        OidcWiremockTestResource.generateJwtToken("bob", Set.of(), "sub", "ID",
                                                Set.of("quarkus-web-app"))
                                        + "\""
                                        + "}")));
    }

    private void defineCodeFlowLogoutStub() {
        wireMockServer.stubFor(
                get(urlPathMatching("/auth/realms/quarkus/protocol/openid-connect/end-session"))
                        .willReturn(aResponse()
                                .withHeader("Location",
                                        "{{request.query.returnTo}}?clientId={{request.query.client_id}}")
                                .withStatus(302)
                                .withTransformers("response-template")));
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }

    private Cookie getStateCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookies().stream()
                .filter(c -> c.getName().startsWith("q_auth" + (tenantId == null ? "" : "_" + tenantId))).findFirst()
                .orElse(null);
    }

    private static void checkResourceMetadata(String resource, String realm) {
        Response metadataResponse = RestAssured.when()
                .get("http://localhost:8081" + OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH
                        + (resource == null ? "" : "/" + resource));
        JsonObject jsonMetadata = new JsonObject(metadataResponse.asString());
        assertEquals("https://localhost:8081" + (resource == null ? "" : "/" + resource),
                jsonMetadata.getString(OidcConstants.RESOURCE_METADATA_RESOURCE));
        JsonArray jsonAuthorizationServers = jsonMetadata.getJsonArray(OidcConstants.RESOURCE_METADATA_AUTHORIZATION_SERVERS);
        assertEquals(1, jsonAuthorizationServers.size());

        String authorizationServer = jsonAuthorizationServers.getString(0);
        assertTrue(authorizationServer.startsWith("http://localhost:"));
        assertTrue(authorizationServer.endsWith("/realms/" + realm));
    }
}
