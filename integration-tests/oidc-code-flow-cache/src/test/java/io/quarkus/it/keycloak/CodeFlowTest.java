package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class CodeFlowTest {

    @Test
    public void testCodeFlowNoConsent() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
            verifyLocationHeader(webClient, webResponse.getResponseHeaderValue("location"), null, "web-app", false);

            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("/index.html", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Welcome to Test App", page.getTitleText(),
                    "A second request should not redirect and just re-authenticate the user");

            page = webClient.getPage("http://localhost:8081/web-app/configMetadataIssuer");

            assertEquals(
                    KeycloakRealmResourceManager.KEYCLOAK_SERVER_URL + "/realms/" + KeycloakRealmResourceManager.KEYCLOAK_REALM,
                    page.asText());

            page = webClient.getPage("http://localhost:8081/web-app/configMetadataScopes");

            assertTrue(page.asText().contains("openid"));
            assertTrue(page.asText().contains("profile"));

            webClient.getCookieManager().clearCookies();
        }
    }

    private void verifyLocationHeader(WebClient webClient, String loc, String tenant, String path, boolean forceHttps) {
        assertTrue(loc.startsWith("http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/auth"));
        String scheme = forceHttps ? "https" : "http";
        assertTrue(loc.contains("redirect_uri=" + scheme + "%3A%2F%2Flocalhost%3A8081%2F" + path));
        assertTrue(loc.contains("state=" + getStateCookieStateParam(webClient, tenant)));
        assertTrue(loc.contains("scope=openid+profile+email+phone"));
        assertTrue(loc.contains("response_type=code"));
        assertTrue(loc.contains("client_id=quarkus-app"));
        assertTrue(loc.contains("max-age=60"));

    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getStateCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_auth" + (tenantId == null ? "" : "_" + tenantId));
    }

    private String getStateCookieStateParam(WebClient webClient, String tenantId) {
        return getStateCookie(webClient, tenantId).getValue().split("\\|")[0];
    }

    private String getStateCookieSavedPath(WebClient webClient, String tenantId) {
        String[] parts = getStateCookie(webClient, tenantId).getValue().split("\\|");
        return parts.length == 2 ? parts[1] : null;
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }

    private Cookie getSessionAtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_at" + (tenantId == null ? "" : "_" + tenantId));
    }

    private Cookie getSessionRtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_rt" + (tenantId == null ? "" : "_" + tenantId));
    }

    private String getIdToken(Cookie sessionCookie) {
        return sessionCookie.getValue().split("\\|")[0];
    }
}
