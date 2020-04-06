package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
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
            webClient.getCookieManager().clearCookies();
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
            Cookie sessionCookie = getSessionCookie(webClient);
            assertNotNull(sessionCookie);
            assertEquals("/", sessionCookie.getPath());

            Thread.sleep(5000);
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(getSessionCookie(webClient));

            webClient.getOptions().setRedirectEnabled(true);
            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());
            webClient.getCookieManager().clearCookies();
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
            webClient.getCookieManager().clearCookies();
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
        }
    }

    @Test
    public void testIdTokenInjectionWithoutRestoredPathDifferentRoot() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app2/callback-before-redirect?tenantId=tenant-2");
            assertNotNull(getStateCookieStateParam(webClient));
            assertNull(getStateCookieSavedPath(webClient));

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("web-app2:alice", page.getBody().asText());

            page = webClient.getPage("http://localhost:8081/web-app2/name");

            assertEquals("web-app2:alice", page.getBody().asText());

            assertNull(getStateCookie(webClient));
            Cookie sessionCookie = getSessionCookie(webClient);
            assertNotNull(sessionCookie);
            assertEquals("/web-app2", sessionCookie.getPath());

            Thread.sleep(5000);
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/web-app2/name").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(getSessionCookie(webClient));

            webClient.getOptions().setRedirectEnabled(true);
            page = webClient.getPage("http://localhost:8081/web-app2/name");

            assertEquals("Log in to quarkus", page.getTitleText());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAuthenticationCompletionFailedNoStateCookie() throws IOException, InterruptedException {
        // tenant-3 configuration uses a '/some/other/path' redirect parameter which does not have the same root
        // as the original request which is 'web-app', as a result, when the user is returned back to Quarkus
        // to '/some/other/path' no state cookie is detected.
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-redirect?tenantId=tenant-3");
            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            try {
                loginForm.getInputByName("login").click();
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAuthenticationCompletionFailedWrongRedirectUri() throws IOException, InterruptedException {
        // CustomTenantResolver will return null for an empty tenantId which will lead to the default configuration
        // being used and result in '/web-app/callback-before-redirect' be used as an initial redirect_uri parameter.
        // When the user is redirected back, CustomTenantResolver will resolve a 'tenant-1' configuration with
        // a redirect_uri '/web-app/callback-after-redirect' which will cause a code to token exchange failure
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-redirect?tenantId");
            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            try {
                page = loginForm.getInputByName("login").click();
                fail("401 status error is expected: " + page.getBody().asText());
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();
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
            webClient.getCookieManager().clearCookies();
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
            webClient.getCookieManager().clearCookies();
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
            webClient.getCookieManager().clearCookies();
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
