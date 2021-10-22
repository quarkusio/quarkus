package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice", page.getBody().asText());
        }
    }

    @Test
    public void testCodeFlowUserInfo() throws IOException {
        defineCodeFlowAuthorizationOauth2TokenStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-user-info");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice", page.getBody().asText());
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private void defineCodeFlowAuthorizationOauth2TokenStub() {
        wireMockServer.stubFor(WireMock.post("/auth/realms/quarkus/oauth2-tokens")
                .withRequestBody(containing("authorization_code"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + OidcWiremockTestResource.getAccessToken("alice", Set.of()) + "\""
                                + "}")));
    }
}
