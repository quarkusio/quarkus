package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

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
    public void testCodeFlowFormPostAndBackChannelLogout() throws Exception {
        testCodeFlowFormPostAndBackChannelLogout("code-flow-form-post", "back-channel-logout");
    }

    @Test
    public void testBackChannelLogoutForDynamicTenants() throws Exception {
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-one", "back-channel-logout/dynamic-tenant-one");
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-two", "back-channel-logout/dynamic-tenant-two");
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

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }
}
