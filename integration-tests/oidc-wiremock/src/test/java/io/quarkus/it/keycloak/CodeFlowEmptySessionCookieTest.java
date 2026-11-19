package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(CustomOidcWiremockTestResource.class)
public class CodeFlowEmptySessionCookieTest {

    private static final String APP_URL = "http://localhost:8081";
    private static final String SESSION_COOKIE = "q_session_code-flow";
    private static final String STATE_COOKIE_PREFIX = "q_auth_code-flow";
    private static final String SPLIT_TOKENS_SESSION_COOKIE = "q_session_code-flow-split-tokens";
    private static final String SPLIT_TOKENS_ACCESS_TOKEN_COOKIE = "q_session_at_code-flow-split-tokens";
    private static final String SPLIT_TOKENS_REFRESH_TOKEN_COOKIE = "q_session_rt_code-flow-split-tokens";

    @OidcWireMock
    WireMockServer wireMockServer;

    @BeforeAll
    public static void clearCache() {
        RestAssured
                .given()
                .get(APP_URL + "/clear-token-cache")
                .then()
                .statusCode(204);
    }

    @Test
    public void testEmptySessionCookieDoesNotBlockCodeFlowCompletion() throws Exception {
        // 1. A request with an empty session cookie gets an authentication challenge
        Response challenge = RestAssured.given().redirects().follow(false)
                .header("Cookie", SESSION_COOKIE + "=")
                .get(APP_URL + "/code-flow");
        String authorizationUri = assertChallenge(challenge);
        // the empty session cookie is not removed again
        assertCookieNotTouched(challenge, SESSION_COOKIE);
        Cookie stateCookie = getStateCookie(challenge);

        // 2. The user authenticates at the OIDC provider which redirects back with the authorization code
        Map<String, String> authorizationParams = getQueryParams(authorizationUri);
        Response login = RestAssured.given().redirects().follow(false)
                .queryParam("redirect_uri", authorizationParams.get("redirect_uri"))
                .queryParam("state", authorizationParams.get("state"))
                .get(wireMockServer.baseUrl() + "/login");
        assertEquals(302, login.statusCode());
        String redirectBackUri = login.getHeader("Location");
        assertTrue(redirectBackUri.startsWith(APP_URL + "/code-flow?"), redirectBackUri);
        assertTrue(redirectBackUri.contains("code="), redirectBackUri);

        // 3. The redirect back carries the state cookie but also the empty session cookie
        // because the user agent has not dropped it yet; the code must be exchanged, not ignored
        Response completed = RestAssured.given().redirects().follow(false)
                .header("Cookie", stateCookie.getName() + "=" + stateCookie.getValue() + "; " + SESSION_COOKIE + "=")
                .get(redirectBackUri);
        assertEquals(302, completed.statusCode());
        assertEquals(APP_URL + "/code-flow", completed.getHeader("Location"));
        String sessionCookieValue = completed.getCookie(SESSION_COOKIE);
        assertNotNull(sessionCookieValue);
        assertFalse(sessionCookieValue.isEmpty());
        assertCookieRemoved(completed, stateCookie.getName());

        // 4. The new session is established
        RestAssured.given().redirects().follow(false)
                .header("Cookie", SESSION_COOKIE + "=" + sessionCookieValue)
                .get(APP_URL + "/code-flow")
                .then().statusCode(200).body(startsWith("alice"));
    }

    @Test
    public void testEmptySessionCookieChunksChallenge() {
        Response challenge = RestAssured.given().redirects().follow(false)
                .header("Cookie", SESSION_COOKIE + "_chunk_1=; " + SESSION_COOKIE + "_chunk_2=")
                .get(APP_URL + "/code-flow");
        assertChallenge(challenge);
        assertNotNull(getStateCookie(challenge));
        // the empty session cookie chunks are not removed again
        assertCookieNotTouched(challenge, SESSION_COOKIE + "_chunk_1");
        assertCookieNotTouched(challenge, SESSION_COOKIE + "_chunk_2");
    }

    @Test
    public void testEmptySessionCookieWithSessionCookieChunks() throws Exception {
        try (WebClient webClient = createWebClient()) {
            login(webClient, "/code-flow", "alice");
            String sessionCookieValue = getCookieValue(webClient, SESSION_COOKIE);
            assertNull(webClient.getCookieManager().getCookie(SESSION_COOKIE + "_chunk_1"),
                    "A single session cookie is expected, the test must be adjusted if the tokens have grown");

            // Present the same session as two chunks
            int half = sessionCookieValue.length() / 2;
            String chunk1 = SESSION_COOKIE + "_chunk_1=" + sessionCookieValue.substring(0, half);
            String chunk2 = SESSION_COOKIE + "_chunk_2=" + sessionCookieValue.substring(half);
            assertAuthenticated("/code-flow", chunk1 + "; " + chunk2, "alice");

            // An empty session cookie means no session, the session cookie chunks are not checked
            Response emptySessionCookieChallenge = RestAssured.given().redirects().follow(false)
                    .header("Cookie", chunk1 + "; " + chunk2 + "; " + SESSION_COOKIE + "=")
                    .get(APP_URL + "/code-flow");
            assertChallenge(emptySessionCookieChallenge);
            // the empty session cookie is not removed again
            assertCookieNotTouched(emptySessionCookieChallenge, SESSION_COOKIE);

            // The session cookie wins over an empty chunk
            assertAuthenticated("/code-flow", SESSION_COOKIE + "=" + sessionCookieValue + "; " + SESSION_COOKIE + "_chunk_1=",
                    "alice");

            // An incomplete session can not be restored, a new challenge is expected
            Response challenge = RestAssured.given().redirects().follow(false)
                    .header("Cookie", chunk1 + "; " + SESSION_COOKIE + "_chunk_2=")
                    .get(APP_URL + "/code-flow");
            assertChallenge(challenge);
            assertCookieRemoved(challenge, SESSION_COOKIE + "_chunk_1");
            assertCookieRemoved(challenge, SESSION_COOKIE + "_chunk_2");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testEmptySplitTokenCookies() throws Exception {
        defineSplitTokensTokenStub();
        try (WebClient webClient = createWebClient()) {
            login(webClient, "/code-flow-split-tokens", "alice:true:true");
            String sessionCookie = SPLIT_TOKENS_SESSION_COOKIE + "=" + getCookieValue(webClient, SPLIT_TOKENS_SESSION_COOKIE);
            String accessTokenCookie = SPLIT_TOKENS_ACCESS_TOKEN_COOKIE + "="
                    + getCookieValue(webClient, SPLIT_TOKENS_ACCESS_TOKEN_COOKIE);
            String refreshTokenCookie = SPLIT_TOKENS_REFRESH_TOKEN_COOKIE + "="
                    + getCookieValue(webClient, SPLIT_TOKENS_REFRESH_TOKEN_COOKIE);

            assertAuthenticated("/code-flow-split-tokens", sessionCookie + "; " + accessTokenCookie + "; " + refreshTokenCookie,
                    "alice:true:true");

            // All the session cookies are empty after a logout in the user agents which have not dropped
            // the removed cookies yet; a new challenge is expected and the cookies are not removed again
            Response challenge = RestAssured.given().redirects().follow(false)
                    .header("Cookie", SPLIT_TOKENS_SESSION_COOKIE + "=; " + SPLIT_TOKENS_ACCESS_TOKEN_COOKIE + "=; "
                            + SPLIT_TOKENS_REFRESH_TOKEN_COOKIE + "=")
                    .get(APP_URL + "/code-flow-split-tokens");
            assertChallenge(challenge);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_SESSION_COOKIE);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_ACCESS_TOKEN_COOKIE);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_REFRESH_TOKEN_COOKIE);

            // An empty session cookie means no session even if the token cookies are valid;
            // nothing is removed again, the next login replaces the token cookies
            challenge = RestAssured.given().redirects().follow(false)
                    .header("Cookie", SPLIT_TOKENS_SESSION_COOKIE + "=; " + accessTokenCookie + "; " + refreshTokenCookie)
                    .get(APP_URL + "/code-flow-split-tokens");
            assertChallenge(challenge);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_SESSION_COOKIE);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_ACCESS_TOKEN_COOKIE);
            assertCookieNotTouched(challenge, SPLIT_TOKENS_REFRESH_TOKEN_COOKIE);

            webClient.getCookieManager().clearCookies();
        }
    }

    private void defineSplitTokensTokenStub() {
        // Unlike the default token stub, this one returns 'expires_in' which is required
        // for the access token cookie to be restored
        wireMockServer.stubFor(post("/auth/realms/quarkus/split-tokens-token")
                .withRequestBody(containing("authorization_code"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \"alice-access-token\",\n" +
                                "  \"expires_in\": 300,\n" +
                                "  \"refresh_token\": \"07e08903-1263-4dd1-9fd1-4a59b0db5283\",\n" +
                                "  \"id_token\": \"{{basic-scheme-id-token 'alice'}}\"\n" +
                                "}")
                        .withTransformers("response-template")));
    }

    private static void login(WebClient webClient, String path, String expectedContentPrefix) throws IOException {
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage page = webClient.getPage(APP_URL + path);

        HtmlForm form = page.getFormByName("form");
        form.getInputByName("username").type("alice");
        form.getInputByName("password").type("alice");

        String content = form.getInputByValue("login").click().getWebResponse().getContentAsString();
        assertTrue(content.startsWith(expectedContentPrefix), content);
    }

    private static void assertAuthenticated(String path, String cookieHeader, String expectedContentPrefix) {
        RestAssured.given().redirects().follow(false)
                .header("Cookie", cookieHeader)
                .get(APP_URL + path)
                .then().statusCode(200).body(startsWith(expectedContentPrefix));
    }

    private String assertChallenge(Response response) {
        assertEquals(302, response.statusCode());
        String authorizationUri = response.getHeader("Location");
        assertTrue(authorizationUri.startsWith(wireMockServer.baseUrl() + "/auth/realms/quarkus"), authorizationUri);
        assertTrue(authorizationUri.contains("response_type=code"), authorizationUri);
        return authorizationUri;
    }

    private static void assertCookieNotTouched(Response response, String cookieName) {
        assertNull(response.getDetailedCookie(cookieName), cookieName + " cookie must not be removed again");
    }

    private static void assertCookieRemoved(Response response, String cookieName) {
        Cookie cookie = response.getDetailedCookie(cookieName);
        assertNotNull(cookie, cookieName + " cookie must be removed");
        assertEquals("", cookie.getValue(), cookieName + " cookie must be removed");
        assertEquals(0, cookie.getMaxAge(), cookieName + " cookie must be removed");
    }

    private static Cookie getStateCookie(Response response) {
        for (Cookie cookie : response.getDetailedCookies()) {
            if (cookie.getName().startsWith(STATE_COOKIE_PREFIX)) {
                return cookie;
            }
        }
        throw new AssertionError("State cookie is not found");
    }

    private static String getCookieValue(WebClient webClient, String cookieName) {
        org.htmlunit.http.Cookie cookie = webClient.getCookieManager().getCookie(cookieName);
        assertNotNull(cookie, cookieName + " cookie is expected");
        assertFalse(cookie.getValue().isEmpty(), cookieName + " cookie must not be empty");
        return cookie.getValue();
    }

    private static Map<String, String> getQueryParams(String uri) {
        Map<String, String> params = new HashMap<>();
        for (String param : uri.substring(uri.indexOf('?') + 1).split("&")) {
            int eq = param.indexOf('=');
            params.put(param.substring(0, eq), URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
