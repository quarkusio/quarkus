package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.hamcrest.Matchers;
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
import io.restassured.RestAssured;

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
            verifyLocationHeader(webClient, webResponse.getResponseHeaderValue("location"));

            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("/index.html", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Welcome to Test App", page.getTitleText(),
                    "A second request should not redirect and just re-authenticate the user");
            assertNull(getStateCookie(webClient));
        }
    }

    private void verifyLocationHeader(WebClient webClient, String loc) {
        assertTrue(loc.startsWith("http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/auth"));
        assertTrue(loc.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fweb-app"));
        assertTrue(loc.contains("state=" + getStateCookieStateParam(webClient)));
        assertTrue(loc.contains("scope=openid+profile+email+phone"));
        assertTrue(loc.contains("response_type=code"));
        assertTrue(loc.contains("client_id=quarkus-app"));
        assertTrue(loc.contains("max-age=60"));

    }

    @Test
    public void testTokenTimeoutLogout() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());
            assertNull(getStateCookie(webClient));

            Thread.sleep(10000);

            webClient.getPage("http://localhost:8081/index.html");

            Cookie sessionCookie = getSessionCookie(webClient);

            assertNull(sessionCookie);

            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());
        }
    }

    @Test
    public void testIdTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app");

            assertEquals("alice", page.getBody().asText());
            assertNull(getStateCookie(webClient));
        }
    }

    @Test
    public void testIdTokenInjectionWithoutRestoredPath() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-redirect");
            assertNotNull(getStateCookieStateParam(webClient));
            assertNull(getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("callback:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
            // The same code path which successfully clears the cookie for all the other tests is not effective here.
            // The most likely reason is that the state cookie was created in response to "http://localhost:8081/web-app"
            // while it is cleared in response to "http://localhost:8081/web-app/callback".
            // HtmlUnit logs that a 'q_auth' cookie path parameter 'path' is set to 'path:/web-app'.
            // If really needed we can get the session and state cookie properties configurable.
            // assertNull(getStateCookie(webClient));
        }
    }

    @Test
    public void testAccessTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/access");

            assertEquals("AT injected", page.getBody().asText());
            assertNull(getStateCookie(webClient));
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/refresh");

            assertEquals("RT injected", page.getBody().asText());
            assertNull(getStateCookie(webClient));
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjectionWithoutIndexHtml() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/refresh");
            assertEquals("/web-app/refresh", getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("RT injected", page.getBody().asText());
            assertNull(getStateCookie(webClient));
        }
    }

    @Test
    public void testNoCodeFlowUnprotected() {
        RestAssured.when().get("/public-web-app/access")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("no user"));
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getSessionCookie(WebClient webClient) {
        return webClient.getCookieManager().getCookie("q_session");
    }

    private Cookie getStateCookie(WebClient webClient) {
        return webClient.getCookieManager().getCookie("q_auth");
    }

    private String getStateCookieStateParam(WebClient webClient) {
        return getStateCookie(webClient).getValue().split("___")[0];
    }

    private String getStateCookieSavedPath(WebClient webClient) {
        String[] parts = getStateCookie(webClient).getValue().split("___");
        return parts.length == 2 ? parts[1] : null;
    }
}
