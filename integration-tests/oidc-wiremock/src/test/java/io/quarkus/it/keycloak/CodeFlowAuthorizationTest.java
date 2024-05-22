package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
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
                    + " id_token audience: https://id.server.example.com,"
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
    }

    @Test
    public void testCodeFlowUserInfoCachedInIdToken() throws Exception {
        // Internal ID token, allow in memory cache = false, cacheUserInfoInIdtoken = true
        defineCodeFlowUserInfoCachedInIdTokenStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            Cookie stateCookie = getStateCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            Date stateCookieDate = stateCookie.getExpires();
            final long nowInSecs = System.currentTimeMillis() / 1000;
            final long sessionCookieLifespan = stateCookieDate.toInstant().getEpochSecond() - nowInSecs;
            // 5 mins is default
            assertTrue(sessionCookieLifespan >= 299 && sessionCookieLifespan <= 304);

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice:alice:alice, cache size: 0, TenantConfigResolver: false", textPage.getContent());

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

            // refresh
            Thread.sleep(3000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken");
            assertEquals("alice:alice:bob, cache size: 0, TenantConfigResolver: false", textPage.getContent());

            idTokenClaims = decryptIdToken(webClient, "code-flow-user-info-github-cached-in-idtoken");
            assertNotNull(idTokenClaims.getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE));

            issuedAt = idTokenClaims.getLong("iat");
            expiresAt = idTokenClaims.getLong("exp");
            assertEquals(305, expiresAt - issuedAt);

            sessionCookie = getSessionCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            date = sessionCookie.getExpires();
            assertTrue(date.toInstant().getEpochSecond() - issuedAt >= 305 + 300);
            assertTrue(date.toInstant().getEpochSecond() - issuedAt <= 305 + 300 + 3);

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
    }

    @Test
    public void testCodeFlowTokenIntrospection() throws Exception {
        defineCodeFlowTokenIntrospectionStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-token-introspection");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice:alice", textPage.getContent());

            // refresh
            Thread.sleep(3000);
            textPage = webClient.getPage("http://localhost:8081/code-flow-token-introspection");
            assertEquals("admin:admin", textPage.getContent());

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

    private JsonObject decryptIdToken(WebClient webClient, String tenantId) throws Exception {
        Cookie sessionCookie = getSessionCookie(webClient, tenantId);
        assertNotNull(sessionCookie);

        SecretKey key = new SecretKeySpec(OidcUtils
                .getSha256Digest("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
                        .getBytes(StandardCharsets.UTF_8)),
                "AES");

        String decryptedSessionCookie = OidcUtils.decryptString(sessionCookie.getValue(), key);

        String encodedIdToken = decryptedSessionCookie.split("\\|")[0];

        return OidcUtils.decodeJwtContent(encodedIdToken);
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
                        .withBasicAuth("quarkus-web-app",
                                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
                        .withRequestBody(containing("refresh_token=refresh1234"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \""
                                        + OidcWiremockTestResource.getAccessToken("bob", Set.of()) + "\""
                                        + "}")));

    }

    private void defineCodeFlowUserInfoCachedInIdTokenStub() {
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
                                        + "\"expires_in\": 305"
                                        + "}")));

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
                                        + "  \"refresh_token\": \"refresh5678\""
                                        + "}")));

        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                        .withRequestBody(containing("refresh_token=refresh5678"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"access_token\": \"admin\""
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
}
