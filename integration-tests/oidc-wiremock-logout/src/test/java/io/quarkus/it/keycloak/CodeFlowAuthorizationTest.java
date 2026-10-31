package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.http.Cookie;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class CodeFlowAuthorizationTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @Test
    public void testDisallowedResourceMetadataRouteDoesNotPreventDynamicTenantCreation() {
        // resource-metadata route is not in allowed-routes, but the tenant enables it;
        // a warning is logged and the tenant is created successfully
        RestAssured.given()
                .when().redirects().follow(false)
                .get("http://localhost:8081/service/resource-metadata-tenant")
                .then()
                .statusCode(302);

        RestAssured.given()
                .when().redirects().follow(false)
                .get("http://localhost:8081/service/resource-metadata-tenant?disabled")
                .then()
                .statusCode(302);
    }

    @Test
    public void testCodeFlowFormPostAndBackChannelLogout() throws Exception {
        testCodeFlowFormPostAndBackChannelLogout("code-flow-form-post", "back-channel-logout");
    }

    @Test
    public void testBackChannelLogoutForDynamicTenants() throws Exception {
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-one", "back-channel-logout/dynamic-tenant-one");
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-two", "back-channel-logout/dynamic-tenant-two");
    }

    @Test
    public void testBackChannelLogoutOfMultipleSessions() throws Exception {
        wireMockServer.resetRequests();
        // each login gets an ID token with its own session id, like Keycloak does for separate browser sessions
        List<StubMapping> stubs = defineCodeFlowTokenStubs(
                getIdToken("alice", "123456", "sid-1"),
                getIdToken("alice", "123456", "sid-2"),
                getIdToken("bob", "654321", "sid-3"));
        try (WebClient aliceBrowser = createWebClient();
                WebClient aliceOtherBrowser = createWebClient();
                WebClient bobBrowser = createWebClient()) {
            assertEquals("alice", login(aliceBrowser, "code-flow-form-post"));
            assertEquals("alice", login(aliceOtherBrowser, "code-flow-form-post"));

            // OIDC provider logs alice out of all her sessions, one logout token per session
            postLogoutToken("back-channel-logout", OidcWiremockTestResource.getLogoutToken("123456", "sid-1"));
            postLogoutToken("back-channel-logout", OidcWiremockTestResource.getLogoutToken("123456", "sid-2"));

            // login of another user must not drop the pending logout tokens
            assertEquals("bob", login(bobBrowser, "code-flow-form-post"));

            assertLoggedOut(aliceBrowser, "code-flow-form-post");
            assertLoggedOut(aliceOtherBrowser, "code-flow-form-post");
            assertLoggedIn(bobBrowser, "code-flow-form-post", "bob");

            // tokens of both alice's sessions are revoked by the security event listener
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                wireMockServer.verify(2, postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                        .withHeader("token-revocation-filter", equalTo("access-token")));
                wireMockServer.verify(2, postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                        .withHeader("token-revocation-filter", equalTo("refresh-token")));
            });
        } finally {
            removeStubs(stubs);
        }
    }

    @Test
    public void testLogoutTokenWithoutSessionIdLogsOutAllSessions() throws Exception {
        wireMockServer.resetRequests();
        List<StubMapping> stubs = defineCodeFlowTokenStubs(
                getIdToken("alice", "123456", "sid-1"),
                getIdToken("alice", "123456", "sid-2"));
        try (WebClient aliceBrowser = createWebClient(); WebClient aliceOtherBrowser = createWebClient()) {
            assertEquals("alice", login(aliceBrowser, "logout-all-sessions"));
            assertEquals("alice", login(aliceOtherBrowser, "logout-all-sessions"));

            String logoutToken = OidcWiremockTestResource.getLogoutToken("123456", null);
            postLogoutToken("back-channel-logout/logout-all-sessions", logoutToken);

            assertLoggedOut(aliceBrowser, "logout-all-sessions");
            assertLoggedOut(aliceOtherBrowser, "logout-all-sessions");
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                wireMockServer.verify(2, postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                        .withHeader("token-revocation-filter", equalTo("access-token")));
                wireMockServer.verify(2, postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                        .withHeader("token-revocation-filter", equalTo("refresh-token")));
            });

            // logout token is still cached but it must not log out a session created after the token was issued,
            // the default token stub issues the ID token at the request time
            removeStubs(stubs);
            long logoutTokenIssuedAt = OidcCommonUtils.decodeJwtContent(logoutToken).getLong("iat");
            await().until(() -> Instant.now().getEpochSecond() > logoutTokenIssuedAt);
            assertEquals("alice", login(aliceBrowser, "logout-all-sessions"));
            assertLoggedIn(aliceBrowser, "logout-all-sessions", "alice");
        } finally {
            removeStubs(stubs);
        }
    }

    private void testCodeFlowFormPostAndBackChannelLogout(String tenantId, String backChannelPath) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/service/" + tenantId);

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("alice", textPage.getContent());

            assertNotNull(getSessionCookie(webClient, tenantId));

            textPage = webClient.getPage("http://localhost:8081/service/" + tenantId);
            assertEquals("alice", textPage.getContent());

            // Session is still active
            assertNotNull(getSessionCookie(webClient, tenantId));

            // ID token subject is `123456`
            // request a back channel logout for some other subject
            RestAssured.given()
                    .when().contentType(ContentType.URLENC)
                    .body("logout_token=" + OidcWiremockTestResource.getLogoutToken("789"))
                    .post("http://localhost:8081/service/" + backChannelPath)
                    .then()
                    .statusCode(200);

            // No logout:
            textPage = webClient.getPage("http://localhost:8081/service/" + tenantId);
            assertEquals("alice", textPage.getContent());
            // Session is still active
            assertNotNull(getSessionCookie(webClient, tenantId));

            wireMockServer.verify(0,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke")));

            // request a back channel logout for the same subject
            RestAssured.given()
                    .when().contentType(ContentType.URLENC).body("logout_token="
                            + OidcWiremockTestResource.getLogoutToken("123456"))
                    .post("http://localhost:8081/service/" + backChannelPath)
                    .then()
                    .statusCode(200);

            // Confirm 302 is returned and the session cookie is null
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/service/" + tenantId).toURL()));
            assertEquals(302, webResponse.getStatusCode());
            String clearSiteData = webResponse.getResponseHeaderValue("clear-site-data");
            assertTrue(clearSiteData.equals("\"cache\",\"cookies\"") || clearSiteData.equals("\"cookies\",\"cache\""));

            assertNull(getSessionCookie(webClient, tenantId));

            Thread.sleep(3000);

            wireMockServer.verify(1,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                            .withHeader("token-revocation-filter", equalTo("access-token")));
            wireMockServer.verify(1,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke"))
                            .withHeader("token-revocation-filter", equalTo("refresh-token")));

            wireMockServer.resetRequests();

            webClient.getCookieManager().clearCookies();
        }
    }

    private String login(WebClient webClient, String tenantId) throws Exception {
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage page = webClient.getPage("http://localhost:8081/service/" + tenantId);
        HtmlForm form = page.getFormByName("form");
        form.getInputByName("username").type("alice");
        form.getInputByName("password").type("alice");
        TextPage textPage = form.getInputByValue("login").click();
        assertNotNull(getSessionCookie(webClient, tenantId));
        return textPage.getContent();
    }

    private WebResponse loadPage(WebClient webClient, String tenantId) throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        return webClient.loadWebResponse(new WebRequest(URI.create("http://localhost:8081/service/" + tenantId).toURL()));
    }

    private void assertLoggedIn(WebClient webClient, String tenantId, String user) throws Exception {
        WebResponse webResponse = loadPage(webClient, tenantId);
        assertEquals(200, webResponse.getStatusCode());
        assertEquals(user, webResponse.getContentAsString());
    }

    private void assertLoggedOut(WebClient webClient, String tenantId) throws Exception {
        assertEquals(302, loadPage(webClient, tenantId).getStatusCode());
        assertNull(getSessionCookie(webClient, tenantId));
    }

    private void postLogoutToken(String backChannelPath, String logoutToken) {
        RestAssured.given()
                .when().contentType(ContentType.URLENC)
                .body("logout_token=" + logoutToken)
                .post("http://localhost:8081/service/" + backChannelPath)
                .then()
                .statusCode(200);
    }

    private static String getIdToken(String user, String sub, String sid) {
        return OidcWiremockTestResource.getIdToken(user, "quarkus-web-app", Map.of("sub", sub, "sid", sid));
    }

    private List<StubMapping> defineCodeFlowTokenStubs(String... idTokens) {
        List<StubMapping> stubs = new ArrayList<>();
        String state = Scenario.STARTED;
        for (int i = 0; i < idTokens.length; i++) {
            String nextState = "login-" + (i + 1);
            stubs.add(wireMockServer.stubFor(post("/auth/realms/quarkus/token")
                    .atPriority(1)
                    .inScenario("multiple-logins")
                    .whenScenarioStateIs(state)
                    .willSetStateTo(nextState)
                    .withRequestBody(containing("authorization_code"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\""
                                    + OidcWiremockTestResource.getAccessToken("alice", Set.of("user", "admin")) + "\","
                                    + "\"refresh_token\":\"07e08903-1263-4dd1-9fd1-4a59b0db5283\","
                                    + "\"id_token\":\"" + idTokens[i] + "\"}"))));
            state = nextState;
        }
        return stubs;
    }

    private void removeStubs(List<StubMapping> stubs) {
        stubs.forEach(wireMockServer::removeStub);
        stubs.clear();
        wireMockServer.resetScenarios();
        wireMockServer.resetRequests();
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }
}
