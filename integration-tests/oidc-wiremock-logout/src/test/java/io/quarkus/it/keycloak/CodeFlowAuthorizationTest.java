package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import com.github.tomakehurst.wiremock.client.WireMock;

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
    public void testBackChannelLogoutForDynamicTenants() throws IOException {
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-one", "back-channel-logout/dynamic-tenant-one");
        testCodeFlowFormPostAndBackChannelLogout("dynamic-tenant-two", "back-channel-logout/dynamic-tenant-two");
    }

    private void testCodeFlowFormPostAndBackChannelLogout(String tenantId, String backChannelPath) throws IOException {
        defineRevokeTokenStubs();
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

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(3))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            try {
                                wireMockServer.verify(2,
                                        postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke")));
                                return true;
                            } catch (Throwable t) {
                                return false;
                            }
                        }
                    });

            wireMockServer.verify(2,
                    postRequestedFor(urlPathMatching("/auth/realms/quarkus/revoke")));
            wireMockServer.resetRequests();

            webClient.getCookieManager().clearCookies();
        }
    }

    private void defineRevokeTokenStubs() {
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/revoke")
                        .withRequestBody(containing("token"))
                        .withRequestBody(containing("token_type_hint=access_token"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)));
        wireMockServer
                .stubFor(WireMock.post("/auth/realms/quarkus/revoke")
                        .withRequestBody(containing("token"))
                        .withRequestBody(containing("token_type_hint=refresh_token"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)));
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
