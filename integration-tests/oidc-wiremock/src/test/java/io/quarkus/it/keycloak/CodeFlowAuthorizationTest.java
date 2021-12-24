package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

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
    public void testCodeFlowUserInfo() throws IOException {
        defineCodeFlowAuthorizationOauth2TokenStub();
        doTestCodeFlowUserInfo("code-flow-user-info-only");
        doTestCodeFlowUserInfo("code-flow-user-info-github");
        doTestCodeFlowUserInfo("code-flow-user-info-dynamic-github");
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

            assertNotNull(getSessionCookie(webClient, tenantId));
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
