package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
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
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class CodeFlowAuthorizationTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @Test
    public void testCodeFlow() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice, cache size: 0", page.getBody().asText());
            assertNotNull(getSessionCookie(webClient, "code-flow"));

            page = webClient.getPage("http://localhost:8081/code-flow/logout");
            assertEquals("Welcome, clientId: quarkus-web-app", page.getBody().asText());
            assertNull(getSessionCookie(webClient, "code-flow"));
            // Clear the post logout cookie
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowEncryptedIdToken() throws IOException {
        doTestCodeFlowEncryptedIdToken("code-flow-encrypted-id-token-jwk");
        doTestCodeFlowEncryptedIdToken("code-flow-encrypted-id-token-pem");
    }

    private void doTestCodeFlowEncryptedIdToken(String tenant) throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-encrypted-id-token/" + tenant);

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("user: alice", page.getBody().asText());
            Cookie sessionCookie = getSessionCookie(webClient, tenant);
            assertNotNull(sessionCookie);
            // default session cookie format: "idtoken|accesstoken|refreshtoken"
            assertTrue(OidcUtils.isEncryptedToken(sessionCookie.getValue().split("\\|")[0]));

            // repeat the call with the session cookie containing the encrypted id token
            page = webClient.getPage("http://localhost:8081/code-flow-encrypted-id-token/" + tenant);
            assertEquals("user: alice", page.getBody().asText());

            webClient.getCookieManager().clearCookies();
        }
    }

    public void testCodeFlowFormPostAndBackChannelLogout() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-form-post");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice", page.getBody().asText());

            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            page = webClient.getPage("http://localhost:8081/code-flow-form-post");
            assertEquals("alice", page.getBody().asText());

            // Session is still active
            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            // request a back channel logout
            RestAssured.given()
                    .when().contentType(ContentType.URLENC).body("logout_token=" + OidcWiremockTestResource.getLogoutToken())
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
    }

    @Test
    public void testCodeFlowFormPostAndFrontChannelLogout() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-form-post");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice", page.getBody().asText());

            assertNotNull(getSessionCookie(webClient, "code-flow-form-post"));

            page = webClient.getPage("http://localhost:8081/code-flow-form-post");
            assertEquals("alice", page.getBody().asText());

            // Session is still active
            Cookie sessionCookie = getSessionCookie(webClient, "code-flow-form-post");
            assertNotNull(sessionCookie);
            JsonObject idTokenClaims = OidcUtils.decodeJwtContent(sessionCookie.getValue().split("\\|")[0]);

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
    }

    @Test
    public void testCodeFlowUserInfo() throws IOException {
        defineCodeFlowAuthorizationOauth2TokenStub();

        doTestCodeFlowUserInfo("code-flow-user-info-only");
        doTestCodeFlowUserInfo("code-flow-user-info-github");
        doTestCodeFlowUserInfo("code-flow-user-info-dynamic-github");

        doTestCodeFlowUserInfoCashedInIdToken();
    }

    private void doTestCodeFlowUserInfo(String tenantId) throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/" + tenantId);

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice:alice, cache size: 1", page.getBody().asText());

            Cookie sessionCookie = getSessionCookie(webClient, tenantId);
            assertNotNull(sessionCookie);
            JsonObject idTokenClaims = OidcUtils.decodeJwtContent(sessionCookie.getValue().split("\\|")[0]);
            assertNull(idTokenClaims.getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE));
            webClient.getCookieManager().clearCookies();
        }
    }

    private void doTestCodeFlowUserInfoCashedInIdToken() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-user-info-github-cached-in-idtoken");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice:alice, cache size: 0", page.getBody().asText());

            Cookie sessionCookie = getSessionCookie(webClient, "code-flow-user-info-github-cached-in-idtoken");
            assertNotNull(sessionCookie);
            JsonObject idTokenClaims = OidcUtils.decodeJwtContent(sessionCookie.getValue().split("\\|")[0]);
            assertNotNull(idTokenClaims.getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE));
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private void defineCodeFlowAuthorizationOauth2TokenStub() {
        wireMockServer.stubFor(WireMock.post("/auth/realms/quarkus/access_token")
                .withHeader("X-Custom", matching("XCustomHeaderValue"))
                .withRequestBody(containing("extra-param=extra-param-value"))
                .withRequestBody(containing("authorization_code"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + OidcWiremockTestResource.getAccessToken("alice", Set.of()) + "\""
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
}
