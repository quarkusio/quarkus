package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class CodeFlowTest {

    KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void testCodeFlowNoConsent() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
            verifyLocationHeader(webClient, webResponse.getResponseHeaderValue("location"), null, "web-app", false);

            Cookie stateCookie = getStateCookie(webClient, null);
            assertNotNull(stateCookie);
            assertEquals(stateCookie.getName(), "q_auth_Default_test_" + getStateCookieStateParam(stateCookie));
            assertNull(stateCookie.getSameSite());

            webClient.getCookieManager().clearCookies();

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
                    client.getAuthServerUrl(),
                    page.asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app/configMetadataScopes");

            assertTrue(page.asNormalizedText().contains("openid"));
            assertTrue(page.asNormalizedText().contains("profile"));

            Cookie sessionCookie = getSessionCookie(webClient, null);
            assertNotNull(sessionCookie);
            assertEquals("lax", sessionCookie.getSameSite());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowScopeError() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");

            // replace scope
            keycloakUrl = keycloakUrl.replace("scope=openid+profile+email+phone", "scope=unknown");

            // response from keycloak
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(keycloakUrl).toURL()));

            String endpointLocation = webResponse.getResponseHeaderValue("location");
            URI endpointLocationUri = URI.create(endpointLocation);

            // response from Quarkus
            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));
            assertEquals(401, webResponse.getStatusCode());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowScopeErrorWithErrorPage() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-https/query?code=b").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");

            // replace scope
            keycloakUrl = keycloakUrl.replace("scope=openid+profile+email+phone", "scope=unknown");

            // response from keycloak
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(keycloakUrl).toURL()));

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocation.startsWith("https"));
            endpointLocation = "http" + endpointLocation.substring(5);
            URI endpointLocationUri = URI.create(endpointLocation);

            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));

            // This is a redirect from quarkus-oidc to the error page
            String endpointErrorLocation = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointErrorLocation.startsWith("https"));

            endpointErrorLocation = "http" + endpointErrorLocation.substring(5);

            HtmlPage page = webClient.getPage(URI.create(endpointErrorLocation).toURL());
            assertEquals("code: b, error: invalid_scope, error_description: Invalid scopes: unknown",
                    page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowForceHttpsRedirectUriAndPkce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            // Verify X-Forwarded-Prefix header is checked during all the redirects
            webClient.addRequestHeader("X-Forwarded-Prefix", "/xforwarded");

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-https").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            verifyLocationHeader(webClient, keycloakUrl, "tenant-https_test", "xforwarded%2Ftenant-https",
                    true);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");

            Cookie stateCookie = getStateCookie(webClient, "tenant-https_test");
            assertNull(stateCookie.getSameSite());
            verifyCodeVerifierAndNonce(stateCookie, keycloakUrl);

            assertTrue(endpointLocation.startsWith("https"));
            endpointLocation = "http" + endpointLocation.substring(5);
            assertTrue(endpointLocation.contains("/xforwarded"));
            endpointLocation = endpointLocation.replace("/xforwarded", "");
            URI endpointLocationUri = URI.create(endpointLocation);
            assertNotNull(endpointLocationUri.getRawQuery());

            webClient.addRequestHeader("X-Forwarded-Prefix", "/xforwarded");
            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));
            assertNull(getStateCookie(webClient, "tenant-https_test"));

            // This is a redirect from quarkus-oidc which drops the query parameters
            String endpointLocationWithoutQuery = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocationWithoutQuery.startsWith("https"));
            assertTrue(endpointLocationWithoutQuery.contains("/xforwarded"));

            endpointLocationWithoutQuery = "http" + endpointLocationWithoutQuery.substring(5);
            endpointLocationWithoutQuery = endpointLocationWithoutQuery.replace("/xforwarded", "");
            URI endpointLocationWithoutQueryUri = URI.create(endpointLocationWithoutQuery);
            assertNull(endpointLocationWithoutQueryUri.getRawQuery());

            page = webClient.getPage(endpointLocationWithoutQueryUri.toURL());
            assertEquals("tenant-https:reauthenticated", page.getBody().asNormalizedText());

            List<Cookie> sessionCookies = verifyTenantHttpTestCookies(webClient);

            assertEquals("strict", sessionCookies.get(0).getSameSite());
            assertEquals("strict", sessionCookies.get(1).getSameSite());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testStateCookieIsPresentButStateParamNot() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-https").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            verifyLocationHeader(webClient, keycloakUrl, "tenant-https_test", "tenant-https",
                    true);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint containing the state and code
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocation.startsWith("https://localhost:8081/tenant-https"));

            String code = getCode(URI.create(endpointLocation).getQuery());

            endpointLocation = "http://localhost:8081/tenant-https";

            // State cookie is present
            Cookie stateCookie = getStateCookie(webClient, "tenant-https_test");
            assertNull(stateCookie.getSameSite());
            verifyCodeVerifierAndNonce(stateCookie, keycloakUrl);

            // Make a call without an extra state query param, status is 401
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(endpointLocation + "?code=" + code).toURL()));
            assertEquals(401, webResponse.getStatusCode());

            // the old state cookie has been removed
            assertNull(webClient.getCookieManager().getCookie(stateCookie.getName()));
            webClient.getCookieManager().clearCookies();
        }
    }

    private String getCode(String query) {
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.startsWith("code")) {
                return pair.split("=")[1];
            }
        }
        fail("Authorization code is missing");
        return null;
    }

    @Test
    public void testCodeFlowForceHttpsRedirectUriWithQueryAndPkce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(
                                    URI.create("http://localhost:8081/tenant-https/query?code=b&kc_idp_hint=google").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            String keycloakUrlQuery = URI.create(keycloakUrl).getQuery();
            assertTrue(keycloakUrlQuery.contains("kc_idp_hint=google"));
            assertFalse(keycloakUrlQuery.contains("code=b"));
            verifyLocationHeader(webClient, keycloakUrl, "tenant-https_test", "tenant-https",
                    true);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocation.startsWith("https"));
            endpointLocation = "http" + endpointLocation.substring(5);
            URI endpointLocationUri = URI.create(endpointLocation);
            assertNotNull(endpointLocationUri.getRawQuery());

            Cookie stateCookie = getStateCookie(webClient, "tenant-https_test");
            verifyCodeVerifierAndNonce(stateCookie, keycloakUrl);

            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));
            assertNull(getStateCookie(webClient, "tenant-https_test"));

            // This is a redirect from quarkus-oidc which drops the code and state query parameters
            String endpointLocationWithoutQuery = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocationWithoutQuery.startsWith("https"));

            endpointLocationWithoutQuery = "http" + endpointLocationWithoutQuery.substring(5);
            URI endpointLocationWithoutQueryUri = URI.create(endpointLocationWithoutQuery);
            assertEquals("code=b", endpointLocationWithoutQueryUri.getRawQuery());

            List<Cookie> sessionCookies = verifyTenantHttpTestCookies(webClient);

            StringBuilder sessionCookieValue = new StringBuilder();
            for (Cookie c : sessionCookies) {
                sessionCookieValue.append(c.getValue());
            }

            SecretKey key = new SecretKeySpec(OidcUtils
                    .getSha256Digest("secret".getBytes(StandardCharsets.UTF_8)),
                    "AES");
            String decryptedSessionCookieValue = OidcUtils.decryptString(sessionCookieValue.toString(), key);

            String encodedIdToken = decryptedSessionCookieValue.split("\\|")[0];

            JsonObject idToken = OidcUtils.decodeJwtContent(encodedIdToken);
            String expiresAt = idToken.getInteger("exp").toString();
            page = webClient.getPage(endpointLocationWithoutQueryUri.toURL());
            String response = page.getBody().asNormalizedText();
            assertTrue(
                    response.startsWith("tenant-https:reauthenticated?code=b&expiresAt=" + expiresAt + "&expiresInDuration="));
            Integer duration = Integer.valueOf(response.substring(response.length() - 1));
            assertTrue(duration > 1 && duration < 5);

            verifyTenantHttpTestCookies(webClient);

            webClient.getCookieManager().clearCookies();
        }
    }

    private List<Cookie> verifyTenantHttpTestCookies(WebClient webClient) {
        List<Cookie> sessionCookies = getSessionCookies(webClient, "tenant-https_test");
        assertNotNull(sessionCookies);
        assertEquals(2, sessionCookies.size());
        assertEquals("q_session_tenant-https_test_chunk_1", sessionCookies.get(0).getName());
        assertEquals("q_session_tenant-https_test_chunk_2", sessionCookies.get(1).getName());
        return sessionCookies;
    }

    @Test
    public void testCodeFlowNonce() throws Exception {
        doTestCodeFlowNonce(false);
        try {
            doTestCodeFlowNonce(true);
            fail("Wrong redirect exception is expected");
        } catch (Exception ex) {
            assertEquals("Unexpected 401", ex.getMessage());
        }
    }

    private void doTestCodeFlowNonce(boolean wrongRedirect) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-nonce").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            verifyLocationHeader(webClient, keycloakUrl, "tenant-nonce", "tenant-nonce", false);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");

            Cookie stateCookie = getStateCookie(webClient, "tenant-nonce");
            verifyNonce(stateCookie, keycloakUrl);

            URI endpointLocationUri = URI.create(endpointLocation);
            if (wrongRedirect) {
                endpointLocationUri = URI.create(
                        "http://localhost:8081"
                                + endpointLocationUri.getRawPath()
                                + "/callback"
                                + "?"
                                + endpointLocationUri.getRawQuery());
            }

            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));

            if (wrongRedirect) {
                assertNull(getStateCookie(webClient, "tenant-nonce"));
                assertEquals(401, webResponse.getStatusCode());
                throw new RuntimeException("Unexpected 401");
            }

            assertEquals(302, webResponse.getStatusCode());
            assertNull(getStateCookie(webClient, "tenant-nonce"));

            // At this point the session cookie is already available, this 2nd redirect only drops
            // OIDC code flow parameters such as `code` and `state`
            List<Cookie> sessionCookies = getSessionCookies(webClient, "tenant-nonce");
            assertNotNull(sessionCookies);
            assertEquals(2, sessionCookies.size());
            assertEquals("q_session_tenant-nonce_chunk_1", sessionCookies.get(0).getName());
            assertEquals("q_session_tenant-nonce_chunk_2", sessionCookies.get(1).getName());

            String endpointLocationWithoutQuery = webResponse.getResponseHeaderValue("location");
            URI endpointLocationWithoutQueryUri = URI.create(endpointLocationWithoutQuery);

            // This request will reach the `TenantNonce` endpoint which will also clear the session.
            page = webClient.getPage(endpointLocationWithoutQueryUri.toURL());
            assertEquals("tenant-nonce:reauthenticated", page.getBody().asNormalizedText());

            // both cookies should be gone now.
            assertNull(getSessionCookies(webClient, "tenant-nonce"));
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowMissingNonce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-nonce").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            verifyLocationHeader(webClient, keycloakUrl, "tenant-nonce", "tenant-nonce", false);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");

            Cookie stateCookie = getStateCookie(webClient, "tenant-nonce");
            verifyNonce(stateCookie, keycloakUrl);

            URI endpointLocationUri = URI.create(endpointLocation);

            String cookieValueWithoutNonce = stateCookie.getValue().split("\\|")[0];
            Cookie stateCookieWithoutNonce = new Cookie(stateCookie.getDomain(), stateCookie.getName(),
                    cookieValueWithoutNonce);
            webClient.getCookieManager().clearCookies();
            webClient.getCookieManager().addCookie(stateCookieWithoutNonce);
            Cookie stateCookie2 = getStateCookie(webClient, "tenant-nonce");
            assertEquals(cookieValueWithoutNonce, stateCookie2.getValue());

            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));
            assertEquals(401, webResponse.getStatusCode());
            assertNull(getStateCookie(webClient, "tenant-nonce"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowForceHttpsRedirectUriAndPkceMissingCodeVerifier() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            // Verify X-Forwarded-Prefix header is checked during all the redirects
            webClient.addRequestHeader("X-Forwarded-Prefix", "/xforwarded");

            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant-https").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");
            verifyLocationHeader(webClient, keycloakUrl, "tenant-https_test", "xforwarded%2Ftenant-https",
                    true);

            HtmlPage page = webClient.getPage(keycloakUrl);

            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");

            Cookie stateCookie = getStateCookie(webClient, "tenant-https_test");
            verifyCodeVerifierAndNonce(stateCookie, keycloakUrl);

            assertTrue(endpointLocation.startsWith("https"));
            endpointLocation = "http" + endpointLocation.substring(5);
            assertTrue(endpointLocation.contains("/xforwarded"));
            endpointLocation = endpointLocation.replace("/xforwarded", "");
            URI endpointLocationUri = URI.create(endpointLocation);
            assertNotNull(endpointLocationUri.getRawQuery());
            webClient.addRequestHeader("X-Forwarded-Prefix", "/xforwarded");

            String cookieValueWithoutCodeVerifier = stateCookie.getValue().split("\\|")[0];
            Cookie stateCookieWithoutCodeVerifier = new Cookie(stateCookie.getDomain(), stateCookie.getName(),
                    cookieValueWithoutCodeVerifier);
            webClient.getCookieManager().clearCookies();
            webClient.getCookieManager().addCookie(stateCookieWithoutCodeVerifier);
            Cookie stateCookie2 = getStateCookie(webClient, "tenant-https_test");
            assertEquals(cookieValueWithoutCodeVerifier, stateCookie2.getValue());

            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));
            assertEquals(401, webResponse.getStatusCode());
            assertNull(getStateCookie(webClient, "tenant-https_test"));

            webClient.getCookieManager().clearCookies();
        }
    }

    private void verifyCodeVerifierAndNonce(Cookie stateCookie, String keycloakUrl) throws Exception {
        String encodedState = stateCookie.getValue().split("\\|")[1];

        byte[] secretBytes = "eUk1p7UB3nFiXZGUXi0uph1Y9p34YhBU".getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(OidcUtils.getSha256Digest(secretBytes), "AES");
        JsonObject json = OidcUtils.decryptJson(encodedState, key);
        String codeVerifier = json.getString("code_verifier");
        String codeChallenge = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(OidcUtils.getSha256Digest(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
        assertTrue(keycloakUrl.contains("code_challenge=" + codeChallenge));
        String nonce = json.getString("nonce");
        assertTrue(keycloakUrl.contains("nonce=" + nonce));
    }

    private void verifyNonce(Cookie stateCookie, String keycloakUrl) throws Exception {
        String encodedState = stateCookie.getValue().split("\\|")[1];

        byte[] secretBytes = "eUk1p7UB3nFiXZGUXi0uph1Y9p34YhBU".getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(OidcUtils.getSha256Digest(secretBytes), "AES");
        JsonObject json = OidcUtils.decryptJson(encodedState, key);
        assertFalse(keycloakUrl.contains("code_challenge="));
        String nonce = json.getString("nonce");
        assertTrue(keycloakUrl.contains("nonce=" + nonce));
    }

    private void verifyLocationHeader(WebClient webClient, String loc, String tenant, String path, boolean httpsScheme) {
        assertTrue(loc.startsWith(client.getAuthServerUrl() + "/protocol/openid-connect/auth"));
        String scheme = httpsScheme ? "https" : "http";
        assertTrue(loc.contains("redirect_uri=" + scheme + "%3A%2F%2Flocalhost%3A8081%2F" + path));
        assertTrue(loc.contains("state=" + getStateCookieStateParam(webClient, tenant)));
        assertTrue(loc.contains("scope=openid+profile+email+phone"));
        assertTrue(loc.contains("response_type=code"));
        assertTrue(loc.contains("client_id=quarkus-app"));
        assertTrue(loc.contains("max-age=60"));

        if (httpsScheme) {
            assertTrue(loc.contains("code_challenge"));
            assertTrue(loc.contains("code_challenge_method=S256"));
        }
    }

    @Test
    public void testTokenTimeoutLogout() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());
            assertNull(getStateCookie(webClient, null));
            assertNull(getSessionAtCookie(webClient, null));
            assertNull(getSessionRtCookie(webClient, null));
            Cookie sessionCookie = getSessionCookie(webClient, null);
            assertNotNull(sessionCookie);
            assertEquals("/", sessionCookie.getPath());
            assertEquals("localhost", sessionCookie.getDomain());

            webClient.getOptions().setRedirectEnabled(false);
            webClient.getCache().clear();

            await().atLeast(6, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(6))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            WebResponse webResponse = webClient
                                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
                            assertEquals(302, webResponse.getStatusCode());
                            assertNull(getSessionCookie(webClient, null));
                            return true;
                        }
                    });

            webClient.getOptions().setRedirectEnabled(true);
            webClient.getCookieManager().clearCookies();
            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Sign in to quarkus", page.getTitleText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testRPInitiatedLogout() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant-logout");
            assertEquals("Sign in to logout-realm", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("Tenant Logout, refreshed: false", page.asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-logout"));

            page = webClient.getPage("http://localhost:8081/tenant-logout/logout");
            assertTrue(page.asNormalizedText().contains("You were logged out, please login again"));
            assertNull(getSessionCookie(webClient, "tenant-logout"));

            page = webClient.getPage("http://localhost:8081/tenant-logout");
            assertEquals("Sign in to logout-realm", page.getTitleText());

            // login again
            loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("Tenant Logout, refreshed: false", page.asNormalizedText());

            assertNotNull(getSessionCookie(webClient, "tenant-logout"));

            await().atLeast(Duration.ofSeconds(11));

            page = webClient.getPage("http://localhost:8081/tenant-logout/logout");
            assertTrue(page.asNormalizedText().contains("You were logged out, please login again"));
            assertNull(getSessionCookie(webClient, "tenant-logout"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testTokenRefresh() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant-refresh");
            assertEquals("Sign in to logout-realm", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("Tenant Refresh, refreshed: false", page.asNormalizedText());

            Cookie sessionCookie = getSessionCookie(webClient, "tenant-refresh");
            assertNotNull(sessionCookie);
            String idToken = getIdToken(sessionCookie);

            //wait now so that we reach the ID token timeout
            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(2))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            webClient.getOptions().setRedirectEnabled(false);
                            WebResponse webResponse = webClient
                                    .loadWebResponse(
                                            new WebRequest(URI.create("http://localhost:8081/tenant-refresh").toURL()));
                            assertEquals(200, webResponse.getStatusCode());
                            // Should not redirect to OP but silently refresh token
                            Cookie newSessionCookie = getSessionCookie(webClient, "tenant-refresh");
                            assertNotNull(newSessionCookie);
                            return webResponse.getContentAsString().equals("Tenant Refresh, refreshed: true")
                                    && !idToken.equals(getIdToken(newSessionCookie));
                        }
                    });

            // local session refreshed and still valid
            page = webClient.getPage("http://localhost:8081/tenant-refresh");
            assertEquals("Tenant Refresh, refreshed: false", page.asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-refresh"));

            //wait now so that we reach the refresh timeout
            await().atMost(20, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(1))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            webClient.getOptions().setRedirectEnabled(false);
                            WebResponse webResponse = webClient
                                    .loadWebResponse(
                                            new WebRequest(URI.create("http://localhost:8081/tenant-refresh").toURL()));
                            // Should redirect to login page given that session is now expired at the OP
                            int statusCode = webResponse.getStatusCode();

                            if (statusCode == 302) {
                                assertNull(getSessionCookie(webClient, "tenant-refresh"));
                                return true;
                            }

                            return false;
                        }
                    });

            // session invalidated because it ended at the OP, should redirect to login page at the OP
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getCookieManager().clearCookies();

            page = webClient.getPage("http://localhost:8081/tenant-refresh");
            assertNull(getSessionCookie(webClient, "tenant-logout"));
            assertEquals("Sign in to logout-realm", page.getTitleText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    // See https://github.com/quarkusio/quarkus/issues/27900
    @Disabled("flaky")
    public void testTokenAutoRefresh() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant-autorefresh");
            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("Tenant AutoRefresh, refreshed: false", page.asNormalizedText());

            Cookie sessionCookie = getSessionCookie(webClient, "tenant-autorefresh");
            assertNotNull(sessionCookie);
            String idToken = getIdToken(sessionCookie);

            // Auto-refresh-interval is 30 secs so every call auto-refreshes the token
            // Right now the original ID token is still valid but will be auto-refreshed
            page = webClient.getPage("http://localhost:8081/tenant-autorefresh");
            assertEquals("Tenant AutoRefresh, refreshed: true", page.asNormalizedText());
            sessionCookie = getSessionCookie(webClient, "tenant-autorefresh");
            assertNotNull(sessionCookie);
            String nextIdToken = getIdToken(sessionCookie);
            assertNotEquals(idToken, nextIdToken);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testIdTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app");

            assertEquals("alice", page.getBody().asNormalizedText());

            Cookie sessionCookie = getSessionCookie(webClient, null);
            assertNotNull(sessionCookie);
            // Replace the session cookie with the correctly formatted cookie but with invalid token values
            webClient.getCookieManager().clearCookies();
            webClient.getCookieManager().addCookie(new Cookie(sessionCookie.getDomain(), sessionCookie.getName(),
                    "1|2|3"));
            sessionCookie = getSessionCookie(webClient, null);
            assertEquals("1|2|3", sessionCookie.getValue());

            try {
                webClient.getPage("http://localhost:8081/web-app");
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
                assertNull(getSessionCookie(webClient, null));
            }
            webClient.getCookieManager().clearCookies();

            // Replace the session cookie with malformed cookie
            webClient.getCookieManager().clearCookies();
            webClient.getCookieManager().addCookie(new Cookie(sessionCookie.getDomain(), sessionCookie.getName(),
                    "1"));
            sessionCookie = getSessionCookie(webClient, null);
            assertEquals("1", sessionCookie.getValue());

            try {
                webClient.getPage("http://localhost:8081/web-app");
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
                assertNull(getSessionCookie(webClient, null));
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testIdTokenInjectionWithoutRestoredPath() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-redirect");
            assertNotNull(getStateCookieStateParam(webClient, "tenant-1"));
            assertNull(getStateCookieSavedPath(webClient, "tenant-1"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("callback:alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testIdTokenInjectionJwtMethod() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/web-app/callback-jwt-before-redirect").toURL()));
            Cookie stateCookie = getNonUniqueStateCookie(webClient, "tenant-jwt");
            assertEquals(stateCookie.getName(), "q_auth_tenant-jwt");
            assertNotNull(getStateCookieStateParam(stateCookie));
            assertNull(getStateCookieSavedPath(stateCookie));

            HtmlPage page = webClient.getPage(webResponse.getResponseHeaderValue("location"));
            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            URI endpointLocationUri = URI.create(webResponse.getResponseHeaderValue("location"));
            assertNotNull(endpointLocationUri.getRawQuery());
            webResponse = webClient.loadWebResponse(new WebRequest(endpointLocationUri.toURL()));

            // This is a redirect from quarkus-oidc which drops the query parameters
            URI endpointLocationUri2 = URI.create(webResponse.getResponseHeaderValue("location"));
            assertNull(endpointLocationUri2.getRawQuery());

            page = webClient.getPage(endpointLocationUri2.toString());
            assertEquals("callback-jwt:alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testIdTokenInjectionJwtMethodMissingStateQueryParam() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/web-app/callback-jwt-before-redirect").toURL()));
            Cookie stateCookie = getNonUniqueStateCookie(webClient, "tenant-jwt");

            assertEquals(stateCookie.getName(), "q_auth_tenant-jwt");
            assertNotNull(getStateCookieStateParam(stateCookie));
            assertNull(getStateCookieSavedPath(stateCookie));

            HtmlPage page = webClient.getPage(webResponse.getResponseHeaderValue("location"));
            assertEquals("Sign in to quarkus", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webResponse = loginForm.getInputByName("login").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // This is a redirect from the OIDC server to the endpoint
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertTrue(endpointLocation.startsWith("http://localhost:8081/web-app/callback-jwt-after-redirect"));
            endpointLocation = "http://localhost:8081/web-app/callback-jwt-after-redirect";
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(endpointLocation).toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNotNull(getNonUniqueStateCookie(webClient, "tenant-jwt"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testIdTokenInjectionJwtMethodButPostMethodUsed() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-jwt-not-used-before-redirect");
            assertNotNull(getStateCookieStateParam(webClient, "tenant-jwt-not-used"));
            assertNull(getStateCookieSavedPath(webClient, "tenant-jwt-not-used"));

            assertEquals("Sign in to quarkus", page.getTitleText());

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
    public void testIdTokenInjectionWithoutRestoredPathDifferentRoot() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app2/callback-before-redirect?tenantId=tenant-2");
            assertNotNull(getStateCookieStateParam(webClient, "tenant-2"));
            assertEquals("?tenantId=tenant-2", getStateCookieSavedPath(webClient, "tenant-2"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("web-app2:alice", page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app2/name");

            assertEquals("web-app2:alice", page.getBody().asNormalizedText());

            assertNull(getStateCookie(webClient, "tenant-2"));
            Cookie sessionCookie = getSessionCookie(webClient, "tenant-2");
            assertNotNull(sessionCookie);
            assertEquals("/web-app2", sessionCookie.getPath());

            Thread.sleep(5000);
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/web-app2/name").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(getSessionCookie(webClient, "tenant-2"));

            webClient.getOptions().setRedirectEnabled(true);
            webClient.getCookieManager().clearCookies();

            page = webClient.getPage("http://localhost:8081/web-app2/name");

            assertEquals("Sign in to quarkus", page.getTitleText());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAuthenticationCompletionFailedNoStateCookie() throws IOException, InterruptedException {
        // tenant-3 configuration uses a '/web-app3' redirect parameter which does not have the same root
        // as the original request which is 'web-app', as a result, when the user is returned back to Quarkus
        // to '/web-app3' no state cookie is detected.
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-redirect?tenantId=tenant-3");
            assertEquals("Sign in to quarkus", page.getTitleText());

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
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/callback-before-wrong-redirect");
            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            try {
                page = loginForm.getInputByName("login").click();
                fail("401 status error is expected: " + page.getBody().asNormalizedText());
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
                assertEquals("http://localhost:8081/web-app/callback-before-wrong-redirect",
                        ex.getResponse().getResponseHeaderValue("RedirectUri"));
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAccessTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/access");

            assertEquals("AT injected", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjection() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");
            assertEquals("/index.html", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/refresh");

            assertEquals("RT injected", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjectionWithoutIndexHtml() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/refresh");
            assertEquals("/web-app/refresh", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("RT injected", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDefaultSessionManagerIdTokenOnly() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/tenant-idtoken-only");
            assertNotNull(getStateCookie(webClient, "tenant-idtoken-only"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-idtoken-only:alice", page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app/access/tenant-idtoken-only");
            assertEquals("tenant-idtoken-only:no access", page.getBody().asNormalizedText());
            page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-idtoken-only");
            assertEquals("tenant-idtoken-only:no refresh", page.getBody().asNormalizedText());

            Cookie idTokenCookie = getSessionCookie(page.getWebClient(), "tenant-idtoken-only");
            checkSingleTokenCookie(idTokenCookie, "ID", "secret");

            assertNull(getSessionAtCookie(webClient, "tenant-idtoken-only"));
            assertNull(getSessionRtCookie(webClient, "tenant-idtoken-only"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDefaultSessionManagerIdRefreshTokens() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/tenant-id-refresh-token");
            assertNotNull(getStateCookie(webClient, "tenant-id-refresh-token"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-id-refresh-token:alice", page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app/access/tenant-id-refresh-token");
            assertEquals("tenant-id-refresh-token:no access", page.getBody().asNormalizedText());
            page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-id-refresh-token");
            assertEquals("tenant-id-refresh-token:RT injected", page.getBody().asNormalizedText());

            Cookie sessionCookie = getSessionCookie(page.getWebClient(), "tenant-id-refresh-token");

            SecretKey key = new SecretKeySpec(OidcUtils
                    .getSha256Digest("secret".getBytes(StandardCharsets.UTF_8)),
                    "AES");

            String sessionCookieValue = OidcUtils.decryptString(sessionCookie.getValue(), key);

            String[] parts = sessionCookieValue.split("\\|");
            assertEquals(3, parts.length);
            assertEquals("ID", OidcUtils.decodeJwtContent(parts[0]).getString("typ"));
            assertEquals("", parts[1]);
            assertEquals("Refresh", OidcUtils.decodeJwtContent(parts[2]).getString("typ"));

            assertNull(getSessionAtCookie(webClient, "tenant-id-refresh-token"));
            assertNull(getSessionRtCookie(webClient, "tenant-id-refresh-token"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDefaultSessionManagerSplitTokens() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/tenant-split-tokens");
            assertNotNull(getStateCookie(webClient, "tenant-split-tokens"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-split-tokens:alice, id token has 5 parts, access token has 5 parts, refresh token has 5 parts",
                    page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app/access/tenant-split-tokens");
            assertEquals("tenant-split-tokens:AT injected", page.getBody().asNormalizedText());
            page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-split-tokens");
            assertEquals("tenant-split-tokens:RT injected", page.getBody().asNormalizedText());

            final String decryptSecret = "eUk1p7UB3nFiXZGUXi0uph1Y9p34YhBU";
            Cookie idTokenCookie = getSessionCookie(page.getWebClient(), "tenant-split-tokens");
            assertEquals("strict", idTokenCookie.getSameSite());
            checkSingleTokenCookie(idTokenCookie, "ID", decryptSecret);

            Cookie atTokenCookie = getSessionAtCookie(page.getWebClient(), "tenant-split-tokens");
            assertEquals("strict", atTokenCookie.getSameSite());
            checkSingleTokenCookie(atTokenCookie, "Bearer", decryptSecret);

            Cookie rtTokenCookie = getSessionRtCookie(page.getWebClient(), "tenant-split-tokens");
            assertEquals("strict", rtTokenCookie.getSameSite());
            checkSingleTokenCookie(rtTokenCookie, "Refresh", decryptSecret);

            // verify all the cookies are cleared after the session timeout
            webClient.getOptions().setRedirectEnabled(false);
            webClient.getCache().clear();

            await().atLeast(6, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(6))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            WebResponse webResponse = webClient
                                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
                            assertEquals(302, webResponse.getStatusCode());
                            assertNull(getSessionCookie(webClient, null));
                            return true;
                        }
                    });

            assertNull(getSessionCookie(page.getWebClient(), "tenant-split-tokens"));
            assertNull(getSessionAtCookie(page.getWebClient(), "tenant-split-tokens"));
            assertNull(getSessionRtCookie(page.getWebClient(), "tenant-split-tokens"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDefaultSessionManagerIdRefreshSplitTokens() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/tenant-split-id-refresh-token");
            assertNotNull(getStateCookie(webClient, "tenant-split-id-refresh-token"));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-split-id-refresh-token:alice", page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/web-app/access/tenant-split-id-refresh-token");
            assertEquals("tenant-split-id-refresh-token:no access", page.getBody().asNormalizedText());
            page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-split-id-refresh-token");
            assertEquals("tenant-split-id-refresh-token:RT injected", page.getBody().asNormalizedText());

            Cookie idTokenCookie = getSessionCookie(page.getWebClient(), "tenant-split-id-refresh-token");
            checkSingleTokenCookie(idTokenCookie, "ID", "secret");

            assertNull(getSessionAtCookie(page.getWebClient(), "tenant-split-id-refresh-token"));

            Cookie rtTokenCookie = getSessionRtCookie(page.getWebClient(), "tenant-split-id-refresh-token");
            checkSingleTokenCookie(rtTokenCookie, "Refresh", "secret");

            // verify all the cookies are cleared after the session timeout
            webClient.getOptions().setRedirectEnabled(false);
            webClient.getCache().clear();

            await().atLeast(6, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(6))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            WebResponse webResponse = webClient
                                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
                            assertEquals(302, webResponse.getStatusCode());
                            assertNull(getSessionCookie(webClient, null));
                            return true;
                        }
                    });

            assertNull(getSessionCookie(page.getWebClient(), "tenant-split-id-refresh-token"));
            assertNull(getSessionAtCookie(page.getWebClient(), "tenant-split-id-refresh-token"));
            assertNull(getSessionRtCookie(page.getWebClient(), "tenant-split-id-refresh-token"));

            webClient.getCookieManager().clearCookies();
        }
    }

    private void checkSingleTokenCookie(Cookie tokenCookie, String type, String decryptSecret) {
        String[] cookieParts = tokenCookie.getValue().split("\\|");
        assertEquals(1, cookieParts.length);
        String token = cookieParts[0];
        String[] tokenParts = token.split("\\.");
        if (decryptSecret != null) {
            assertEquals(5, tokenParts.length);
            try {
                SecretKey key = new SecretKeySpec(OidcUtils
                        .getSha256Digest(decryptSecret.getBytes(StandardCharsets.UTF_8)),
                        "AES");
                token = OidcUtils.decryptString(token, key);
                tokenParts = token.split("\\.");
            } catch (Exception ex) {
                fail("Token decryption has failed");
            }
        }
        assertEquals(3, tokenParts.length);
        JsonObject json = OidcUtils.decodeJwtContent(token);
        assertEquals(type, json.getString("typ"));
    }

    @Test
    public void testAccessAndRefreshTokenInjectionWithoutIndexHtmlAndListener() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            doTestAccessAndRefreshTokenInjectionWithoutIndexHtmlAndListener(webClient);
            webClient.getCookieManager().clearCookies();
        }
    }

    private void doTestAccessAndRefreshTokenInjectionWithoutIndexHtmlAndListener(WebClient webClient)
            throws IOException, InterruptedException {
        HtmlPage page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-listener");

        assertEquals("Sign in to quarkus", page.getTitleText());

        HtmlForm loginForm = page.getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute("alice");
        loginForm.getInputByName("password").setValueAttribute("alice");

        page = loginForm.getInputByName("login").click();

        assertEquals("RT injected(event:OIDC_LOGIN,tenantId:tenant-listener,blockingApi:true)",
                page.getBody().asNormalizedText());
    }

    @Test
    public void testAccessAndRefreshTokenInjectionWithoutIndexHtmlAndListenerMultiTab() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/refresh/tenant-listener");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            doTestAccessAndRefreshTokenInjectionWithoutIndexHtmlAndListener(webClient);

            try {
                page = loginForm.getInputByName("login").click();
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(400, ex.getStatusCode());
                assertTrue(ex.getResponse().getContentAsString().contains("You are already logged in"));
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjectionWithQuery() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/web-app/refresh-query?a=aValue");
            assertEquals("/web-app/refresh-query?a=aValue", getStateCookieSavedPath(webClient, null));

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("RT injected:aValue", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testRedirectUriWithForwardedPrefix() throws IOException, InterruptedException {
        //doTestRedirectUriWithForwardedPrefix("/service");
        doTestRedirectUriWithForwardedPrefix("/service/");
        //doTestRedirectUriWithForwardedPrefix("");
        //doTestRedirectUriWithForwardedPrefix("/");
        //doTestRedirectUriWithForwardedPrefix("//");
    }

    private void doTestRedirectUriWithForwardedPrefix(String prefix) throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            webClient.addRequestHeader("X-Forwarded-Prefix", prefix);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/index.html").toURL()));
            String loc = webResponse.getResponseHeaderValue("location");
            String encodedPrefix;
            if (prefix.isEmpty() || prefix.equals("/") || prefix.equals("//")) {
                encodedPrefix = "%2F";
            } else {
                encodedPrefix = prefix.replaceAll("\\/", "%2F");
            }
            assertTrue(loc.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8081" + encodedPrefix + "web-app"));
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testJavaScriptRequest() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            try {
                webClient.addRequestHeader("X-Requested-With", "JavaScript");
                webClient.getPage("http://localhost:8081/tenant-javascript");
                fail("499 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(499, ex.getStatusCode());
                assertEquals("OIDC", ex.getResponse().getResponseHeaderValue("WWW-Authenticate"));
            }

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCookiePathHeader() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            webClient.addRequestHeader("X-Forwarded-Prefix", "/x-forwarded-prefix-value");
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8081/tenant-cookie-path-header").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertEquals("/x-forwarded-prefix-value", getStateCookie(webClient, "tenant-cookie-path-header").getPath());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testNoCodeFlowUnprotected() {
        RestAssured.when().get("/public-web-app/name")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("no user"));
    }

    @Test
    public void testCustomLogin() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/public-web-app/login");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getStateCookie(WebClient webClient, String tenantId) {
        String cookieSuffix = "q_auth" + (tenantId == null ? "_Default_test" : "_" + tenantId) + "_";
        for (Cookie c : webClient.getCookieManager().getCookies()) {
            if (c.getName().startsWith(cookieSuffix) && c.getName().length() > cookieSuffix.length()) {
                return c;
            }
        }
        return null;
    }

    private Cookie getNonUniqueStateCookie(WebClient webClient, String tenantId) {
        String cookieName = "q_auth" + (tenantId == null ? "_Default_test" : "_" + tenantId);
        return webClient.getCookieManager().getCookie(cookieName);
    }

    private String getStateCookieStateParam(WebClient webClient, String tenantId) {
        return getStateCookie(webClient, tenantId).getValue().split("\\|")[0];
    }

    private String getStateCookieStateParam(Cookie stateCookie) {
        return stateCookie.getValue().split("\\|")[0];
    }

    private String getStateCookieSavedPath(WebClient webClient, String tenantId) {
        String[] parts = getStateCookie(webClient, tenantId).getValue().split("\\|");
        return parts.length == 2 ? parts[1] : null;
    }

    private String getStateCookieSavedPath(Cookie stateCookie) {
        String[] parts = stateCookie.getValue().split("\\|");
        return parts.length == 2 ? parts[1] : null;
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "_Default_test" : "_" + tenantId));
    }

    private List<Cookie> getSessionCookies(WebClient webClient, String tenantId) {
        String sessionCookieNameChunk = "q_session" + (tenantId == null ? "_Default_test" : "_" + tenantId) + "_chunk_";
        CookieManager cookieManager = webClient.getCookieManager();
        SortedMap<String, Cookie> sessionCookies = new TreeMap<>();
        for (Cookie cookie : cookieManager.getCookies()) {
            if (cookie.getName().startsWith(sessionCookieNameChunk)) {
                sessionCookies.put(cookie.getName(), cookie);
            }
        }

        return sessionCookies.isEmpty() ? null : new ArrayList<Cookie>(sessionCookies.values());
    }

    private Cookie getSessionAtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_at" + (tenantId == null ? "_Default_test" : "_" + tenantId));
    }

    private Cookie getSessionRtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_rt" + (tenantId == null ? "_Default_test" : "_" + tenantId));
    }

    private String getIdToken(Cookie sessionCookie) {
        return sessionCookie.getValue().split("\\|")[0];
    }
}
