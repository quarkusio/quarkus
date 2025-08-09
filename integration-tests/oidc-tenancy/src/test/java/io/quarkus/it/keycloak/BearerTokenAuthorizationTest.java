package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.BearerTokenStepUpAuthenticationTest.getAccessTokenWithAcr;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class BearerTokenAuthorizationTest {

    private KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void testResolveTenantIdentifierWebApp() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            assertNotNull(getStateCookie(webClient, "tenant-web-app"));
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            // First call after a redirect, tenant-id is initially calculated from the state `q_auth` cookie.
            // 'reauthenticated' flag is set is because, in fact, it is actually a 2nd call due to
            // quarkus-oidc doing a final redirect after completing a code flow to drop the redirect OIDC parameters
            assertEquals("tenant-web-app:alice:reauthenticated", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-web-app"));
            assertNull(getStateCookie(webClient, "tenant-web-app"));

            // Second call after a redirect, tenant-id is calculated from the state `q_session` cookie
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            assertEquals("tenant-web-app:alice:reauthenticated", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-web-app"));
            assertNull(getStateCookie(webClient, "tenant-web-app"));

            // Local logout
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp?logout=true");
            assertEquals("tenant-web-app:alice:reauthenticated:logout", page.getBody().asNormalizedText());
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            assertNull(getStateCookie(webClient, "tenant-web-app"));

            // Check a new login is requested via redirect
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant/tenant-web-app/api/user/webapp").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            assertNotNull(getStateCookie(webClient, "tenant-web-app"));
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));

            webClient.getCookieManager().clearCookies();

            checkHealth();
        }
    }

    @Test
    public void testFormPostLogoutWebApp() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app:alice:reauthenticated", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-web-app"));

            // First RP initiated form-post logout check
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(
                            new WebRequest(URI.create("http://localhost:8081/tenant/tenant-web-app/form-post-logout").toURL()));
            // Session cookie must be null
            assertNull(getSessionCookie(webClient, "tenant-web-app"));

            assertEquals(200, webResponse.getStatusCode());
            String formPostLogout = webResponse.getContentAsString();
            assertTrue(formPostLogout.startsWith("<html>"));
            assertTrue(formPostLogout.contains("<form method=\"post\" action=\"http://localhost:8081/oidc/form-post-logout\""));
            assertTrue(formPostLogout.endsWith("</html>"));
            // Re-login
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getCookieManager().clearCookies();

            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app:alice:reauthenticated", page.getBody().asNormalizedText());
            // Session cookie must not be null
            assertNotNull(getSessionCookie(webClient, "tenant-web-app"));

            // Complete RP initiated form-post logout
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/form-post-logout");
            assertEquals("alice, you have been logged out with the form post logout", page.getBody().asNormalizedText());
            // Session cookie must be null
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            webClient.getCookieManager().clearCookies();
        }
    }

    private static void checkHealth() {
        RestAssured.when().get("http://localhost:8081/q/health/ready").then().statusCode(404);
    }

    @Test
    public void testJavaScriptRequest() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            try {
                webClient.addRequestHeader("HX-Request", "true");
                webClient.getPage("http://localhost:8081/tenant/tenant-web-app-javascript/api/user/webapp");
                fail("499 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(499, ex.getStatusCode());
                assertEquals("OIDC", ex.getResponse().getResponseHeaderValue("WWW-Authenticate"));
            }

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testResolveTenantIdentifierWebApp2() throws IOException {
        testTenantWebApp2("webapp2", "tenant-web-app2:alice");
    }

    @Test
    public void testScopePermissionsFromAccessToken() throws IOException {
        // source of permissions is access token
        testTenantWebApp2("webapp2-scope-permissions", "email openid profile");
    }

    private void testTenantWebApp2(String webApp2SubPath, String expectedResult) throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user/" + webApp2SubPath);
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Sign in to quarkus-webapp2", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals(expectedResult, page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCodeFlowRefreshTokens() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant-refresh/tenant-web-app-refresh/api/user");
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();

            assertEquals("userName: alice, idToken: true, accessToken: true, refreshToken: true",
                    page.getBody().asNormalizedText());

            Cookie sessionCookie = getSessionCookie(page.getWebClient(), "tenant-web-app-refresh");
            assertNotNull(sessionCookie);
            assertNotNull(getSessionAtCookie(page.getWebClient(), "tenant-web-app-refresh"));
            Cookie rtCookie = getSessionRtCookie(page.getWebClient(), "tenant-web-app-refresh");
            assertNotNull(rtCookie);

            // Wait till the session expires - which should cause the first and also last token refresh request,
            // id and access tokens should have new values, refresh token value should remain the same.
            // No new sign-in process is required.
            //await().atLeast(6, TimeUnit.SECONDS);
            Thread.sleep(2 * 1000);

            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(
                            URI.create("http://localhost:8081/tenant-refresh/tenant-web-app-refresh/api/user")
                                    .toURL()));
            assertEquals("userName: alice, idToken: true, accessToken: true, refreshToken: true",
                    webResponse.getContentAsString());

            Cookie sessionCookie2 = getSessionCookie(webClient, "tenant-web-app-refresh");
            assertNotNull(sessionCookie2);
            assertEquals(sessionCookie2.getValue(), sessionCookie.getValue());
            assertNotNull(getSessionAtCookie(webClient, "tenant-web-app-refresh"));
            Cookie rtCookie2 = getSessionRtCookie(webClient, "tenant-web-app-refresh");
            assertNotNull(rtCookie2);
            assertEquals(rtCookie2.getValue(), rtCookie.getValue());

            //Verify all the cookies are cleared after the session timeout
            webClient.getCache().clear();

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(1))
                    .until(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            webClient.getOptions().setRedirectEnabled(false);
                            WebResponse webResponse = webClient
                                    .loadWebResponse(new WebRequest(
                                            URI.create("http://localhost:8081/tenant-refresh/tenant-web-app-refresh/api/user")
                                                    .toURL()));
                            // Should redirect to login page given that session is now expired and
                            // the 2nd refresh token is expected to fail in the test OidcResource
                            return 302 == webResponse.getStatusCode();
                        }
                    });

            assertNull(getSessionCookie(webClient, "tenant-web-app-refresh"));
            assertNull(getSessionAtCookie(webClient, "tenant-web-app-refresh"));
            assertNull(getSessionRtCookie(webClient, "tenant-web-app-refresh"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testHybridWebApp() throws IOException {
        RestAssured.when().post("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenants/tenant-hybrid/api/user");
            assertNotNull(getStateCookie(webClient, "tenant-hybrid-webapp"));
            assertEquals("Sign in to quarkus-hybrid", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("alice:web-app", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
        RestAssured.when().get("/oidc/userinfo-endpoint-call-count").then().body(equalTo("1"));
    }

    @Test
    public void testOidcClientMultipleAudiences() throws IOException {
        RestAssured.when().get("/oidc-client/multiple-audiences").then().body(equalTo("audience1,audience2"));
    }

    @Test
    public void testHybridService() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "hybrid"))
                .when().get("/tenants/tenant-hybrid/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice:service"));
    }

    @Test
    public void testHybridWebAppService() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenants/tenant-hybrid-webapp-service/api/user");
            assertNotNull(getStateCookie(webClient, "tenant-hybrid-webapp-service"));
            assertEquals("Sign in to quarkus-hybrid", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("alice:web-app", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
        RestAssured.given().auth().oauth2(getAccessToken("alice", "hybrid"))
                .when().get("/tenants/tenant-hybrid-webapp-service/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice:service"));
    }

    @Test
    public void testDefaultClientScopeAsPermission() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "hybrid"))
                .when().get("/tenants/tenant-hybrid-webapp-service/api/mp-scope")
                .then()
                .statusCode(200)
                .body(equalTo("microprofile-jwt"));
        RestAssured.given().auth().oauth2(getAccessToken("alice", "hybrid"))
                .when().get("/tenants/tenant-hybrid-webapp-service/api/non-existent-scope")
                .then()
                .statusCode(403);
    }

    @Test
    public void testResolveTenantIdentifierWebAppNoDiscovery() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/tenant/tenant-web-app-no-discovery/api/user/webapp-no-discovery");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app-no-discovery"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app-no-discovery:alice", page.getBody().asNormalizedText());

            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app-no-discovery/api/user/webapp-no-discovery");
            assertEquals("tenant-web-app-no-discovery:alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testReAuthenticateWhenSwitchingTenants() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            // tenant-web-app
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app:alice:reauthenticated", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "tenant-web-app"));
            // tenant-web-app2
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user/webapp2");
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Sign in to quarkus-webapp2", page.getTitleText());
            loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app2:alice", page.getBody().asNormalizedText());
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            Cookie sessionCookie = getSessionCookie(webClient, "tenant-web-app2");
            assertNotNull(sessionCookie);
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testTenantBAllClients() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-b2/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-b2:alice"));

        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b2"))
                .when().get("/tenant/tenant-b2/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-b2:alice"));

        // should give a 401 given that access token from issuer c can not access tenant b
        RestAssured.given().auth().oauth2(getAccessToken("alice", "c"))
                .when().get("/tenant/tenant-b2/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testResolveTenantIdentifier() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-b/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-b:alice"));

        // should give a 401 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-c/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testCustomHeader() {
        RestAssured.given().header("X-Forwarded-Authorization", getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-customheader/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-customheader:alice"));
    }

    @Test
    public void testCustomHeaderBearerScheme() {
        RestAssured.given().header("X-Forwarded-Authorization", "Bearer " + getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-customheader/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-customheader:alice"));
    }

    @Test
    public void testWrongCustomHeader() {
        RestAssured.given().header("X-Authorization", getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-customheader/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testCustomHeaderCustomScheme() {
        RestAssured.given().header("X-Forwarded-Authorization", "DPoP " + getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-customheader/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testResolveTenantConfig() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "d", "d", List.of("openid")))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-d:alice.alice"));

        // should give a 401 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b", List.of("openid")))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testResolveTenantConfigNoDiscovery() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b", List.of("openid")))
                .when().get("/tenant/tenant-b-no-discovery/api/user/no-discovery")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-b-no-discovery:alice.alice"));
    }

    @Test
    public void testDefaultTenant() {
        // any non-extent tenant should accept tokens from tenant a
        RestAssured.given().auth().oauth2(getAccessToken("alice", "a"))
                .when().get("/tenant/tenant-any/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-any:alice"));
    }

    @Test
    public void testSimpleOidcJwtWithJwkRefresh() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/revoke-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/disable-discovery").then().body(equalTo("false"));
        // Quarkus OIDC is initialized with JWK set with kid '1' as part of the discovery process
        // Now enable the rotation
        RestAssured.when().post("/oidc/enable-rotate").then().body(equalTo("true"));

        // JWK is available now in Quarkus OIDC, confirm that no timeout is needed
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("2"))
                .when().get("/tenant/tenant-oidc/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-oidc:alice"));

        // Get a token with kid '3' - it can only be verified via the introspection fallback since OIDC returns JWK set with kid '2'
        // 401 since the introspection is not enabled
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("3"))
                .when().get("/tenant/tenant-oidc/api/user")
                .then()
                .statusCode(401);

        // Enable introspection
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        // No timeout is required
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("3"))
                .when().get("/tenant/tenant-oidc/api/user?revoke=true")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-oidc:alice"));

        // Finally try the opaque token
        RestAssured.given().auth().oauth2(getOpaqueAccessTokenFromSimpleOidc())
                .when().get("/tenant-opaque/tenant-oidc/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-oidc-opaque:alice:user:user@gmail.com"));

        // OIDC JWK endpoint must've been called only twice, once as part of the Quarkus OIDC initialization
        // and once during the 1st request with a token kid '2', follow up requests must've been blocked due to the interval
        // restrictions
        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("2"));
        // both requests with kid `3` and with the opaque token required the remote introspection
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("3"));
        RestAssured.when().get("/oidc/revoke-endpoint-call-count").then().body(equalTo("1"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/enable-discovery").then().body(equalTo("true"));
        RestAssured.when().post("/oidc/disable-rotate").then().body(equalTo("false"));
    }

    @Test
    public void testJwtTokenIntrospectionDisallowed() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        // Quarkus OIDC is initialized with JWK set with kid '1' as part of the discovery process
        // Now enable the rotation
        RestAssured.when().post("/oidc/enable-rotate").then().body(equalTo("true"));

        // JWK is available now in Quarkus OIDC, confirm that no timeout is needed
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("2"))
                .when().get("/tenant/tenant-oidc-no-introspection/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-oidc-no-introspection:alice"));

        // Enable OIDC introspection endpoint
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("3"))
                .when().get("/tenant/tenant-oidc-no-introspection/api/user")
                .then()
                .statusCode(401);

        // OIDC JWK endpoint must've been called only twice, once as part of the Quarkus OIDC initialization
        // and once during the 1st request with a token kid '2', follow up requests must've been blocked due to the interval
        // restrictions
        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("2"));
        // JWT introspection is disallowed
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-rotate").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
    }

    @Test
    public void testJwtTokenIntrospectionOnlyAndUserInfo() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/cache/clear").then().body(equalTo("0"));

        // Caching token introspection and userinfo is not allowed for this tenant,
        // 3 calls to introspection and user info endpoints are expected.
        // Cache size must stay 0.
        for (int i = 0; i < 3; i++) {
            // unique token is created each time
            RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("2"))
                    .when().get("/tenant/tenant-oidc-introspection-only/api/user")
                    .then()
                    .statusCode(200)
                    .body(equalTo(
                            "tenant-oidc-introspection-only:alice,client_id:client-introspection-only,"
                                    + "introspection_client_id:none,introspection_client_secret:none,active:true,userinfo:alice,cache-size:0"));
        }

        // verifies empty scope claim makes no difference (e.g. doesn't cause NPE)
        RestAssured.given().auth().oauth2(getAccessTokenWithEmptyScopeFromSimpleOidc("2"))
                .when().get("/tenant/tenant-oidc-introspection-only/api/user")
                .then()
                .statusCode(200)
                .body(equalTo(
                        "tenant-oidc-introspection-only:alice,client_id:client-introspection-only,"
                                + "introspection_client_id:none,introspection_client_secret:none,active:true,userinfo:alice,cache-size:0"));

        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("987654321", "2"))
                .when().get("/tenant/tenant-oidc-introspection-only/api/user")
                .then()
                .statusCode(401);

        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("5"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().get("/oidc/userinfo-endpoint-call-count").then().body(equalTo("5"));
        RestAssured.when().get("/cache/size").then().body(equalTo("0"));
    }

    @Test
    public void testNoUserInfoCallIfTokenIsInvalid() {
        RestAssured.when().post("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/cache/clear").then().body(equalTo("0"));

        String jwt = getAccessTokenFromSimpleOidc("2");
        // Modify the signature
        jwt += "-invalid";
        RestAssured.given().auth().oauth2(jwt)
                .when().get("/tenant/tenant-oidc-introspection-only/api/user")
                .then()
                .statusCode(401);

        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("1"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().get("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().get("/cache/size").then().body(equalTo("0"));
    }

    @Test
    public void testJwtTokenIntrospectionOnlyAndUserInfoCache() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().get("/cache/size").then().body(equalTo("0"));

        // Max cache size is 3
        String token1 = getAccessTokenFromSimpleOidc("2");
        // 3 calls are made, only 1 call to introspection and user info endpoints is expected, and only one entry in the cache is expected
        verifyTokenIntrospectionAndUserInfoAreCached(token1, 1);
        String token2 = getAccessTokenFromSimpleOidc("2");
        assertNotEquals(token1, token2);
        // next 3 calls are made, only 1 call to introspection and user info endpoints is expected, and only two entries in the cache are expected
        verifyTokenIntrospectionAndUserInfoAreCached(token2, 2);
        String token3 = getAccessTokenFromSimpleOidc("2");
        assertNotEquals(token1, token3);
        assertNotEquals(token2, token3);
        // next 3 calls are made, only 1 call to introspection and user info endpoints is expected, and only three entries in the cache are expected
        verifyTokenIntrospectionAndUserInfoAreCached(token3, 3);

        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().get("/cache/size").then().body(equalTo("3"));
    }

    private void verifyTokenIntrospectionAndUserInfoAreCached(String token1, int expectedCacheSize) {
        // Each token is unique, each sequence of 3 calls should only result in a single introspection endpoint call
        for (int i = 0; i < 3; i++) {
            RestAssured.given().auth().oauth2(token1)
                    .when().get("/tenant/tenant-oidc-introspection-only-cache/api/user")
                    .then()
                    .statusCode(200)
                    .body(equalTo(
                            "tenant-oidc-introspection-only-cache:alice,client_id:client-introspection-only-cache,"
                                    + "introspection_client_id:bob,introspection_client_secret:bob_secret,active:true,userinfo:alice,cache-size:"
                                    + expectedCacheSize));
        }
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("1"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().get("/oidc/userinfo-endpoint-call-count").then().body(equalTo("1"));
        RestAssured.when().post("/oidc/userinfo-endpoint-call-count").then().body(equalTo("0"));
    }

    @Test
    public void testSimpleOidcNoDiscovery() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/disable-rotate").then().body(equalTo("false"));

        // Quarkus OIDC is initialized with JWK set with kid '1' as part of the initialization process
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("1"))
                .when().get("/tenant/tenant-oidc-no-discovery/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-oidc-no-discovery:alice"));
        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("1"));
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
    }

    @Test
    public void testOpaqueTokenIntrospectionDisallowed() {
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));

        // Verify the the opaque token is rejected with 401
        RestAssured.given().auth().oauth2(getOpaqueAccessTokenFromSimpleOidc())
                .when().get("/tenant-opaque/tenant-oidc-no-opaque-token/api/user")
                .then()
                .statusCode(401);

        // Confirm no introspection request has been made
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
    }

    @Test
    public void testResolveTenantIdentifierWebAppDynamic() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app-dynamic/api/user/webapp");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app-dynamic configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app-dynamic"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("tenant-web-app-dynamic:alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testBothGlobalAndTenantSpecificJwtValidator() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim/api/user")
                .then()
                .statusCode(200);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim/api/user")
                .then()
                .statusCode(401);
        RestAssured.given().auth().oauth2(getAccessToken("admin", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGlobalJwtValidator() {
        // tests that tenant-specific validator is not applied as the @TenantFeature value is not matched
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim-alternative/api/user")
                .then()
                .statusCode(200);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim-alternative/api/user")
                .then()
                .statusCode(401);
        RestAssured.given().auth().oauth2(getAccessToken("admin", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim-alternative/api/user")
                .then()
                .statusCode(200);
    }

    @Test
    public void testRequiredClaimPass() {
        //Client id should match the required azp claim
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b"))
                .when().get("/tenant/tenant-requiredclaim/api/user")
                .then()
                .statusCode(200);
    }

    @Test
    public void testRequiredClaimFail() {
        //Client id does not match required azp claim
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b", "b2"))
                .when().get("/tenant/tenant-requiredclaim/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testOpaqueTokenScopePermission() {
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/cache/clear").then().body(equalTo("0"));

        // verify introspection scopes are mapped to the StringPermissions
        RestAssured.given().auth().oauth2(getOpaqueAccessTokenFromSimpleOidc())
                .when().get("/tenant-opaque/tenant-oidc/api/user-permission")
                .then()
                .statusCode(200)
                .body(equalTo("user"));
        RestAssured.given().auth().oauth2(getOpaqueAccessTokenFromSimpleOidc())
                .when().get("/tenant-opaque/tenant-oidc/api/admin-permission")
                .then()
                .statusCode(403);
    }

    @Test
    public void testTokenIntrospectionRequiredClaims() {
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/oidc/opaque-token-call-count").then().body(equalTo("0"));

        // Successful request with opaque token 2
        String opaqueToken2 = getOpaqueAccessToken2FromSimpleOidc();
        RestAssured.given().auth().oauth2(opaqueToken2)
                .when().get("/tenant-introspection/tenant-introspection-required-claims")
                .then()
                .statusCode(200)
                .body(equalTo("alice, required_claim:1"));

        // Expected to fail now because its introspection does not include the expected required claim
        RestAssured.given().auth().oauth2(opaqueToken2)
                .when().get("/tenant-introspection/tenant-introspection-required-claims")
                .then()
                .statusCode(401);

        RestAssured.when().post("/oidc/opaque-token-call-count").then().body(equalTo("0"));
    }

    @Test
    public void testMultipleTokenIntrospectionRequiredClaims() {
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/oidc/opaque-token-3-call-count").then().body(equalTo("0"));
        String opaqueToken3 = getOpaqueAccessToken3FromSimpleOidc();

        // Expected to fail now because its introspection does include the expected required claim, but value is "1"
        RestAssured.given().auth().oauth2(opaqueToken3)
                .when().get("/tenant-introspection/tenant-introspection-multiple-required-claims")
                .then()
                .statusCode(401);

        // Expected to fail now because its introspection does include the expected required claim, but value is ["1"]
        RestAssured.given().auth().oauth2(opaqueToken3)
                .when().get("/tenant-introspection/tenant-introspection-multiple-required-claims")
                .then()
                .statusCode(401);

        // Expected to fail now because its introspection does not include the expected required claim
        RestAssured.given().auth().oauth2(opaqueToken3)
                .when().get("/tenant-introspection/tenant-introspection-multiple-required-claims")
                .then()
                .statusCode(401);

        // Successful request with opaque token 3 because the required claim is ["1","2"]
        RestAssured.given().auth().oauth2(opaqueToken3)
                .when().get("/tenant-introspection/tenant-introspection-multiple-required-claims")
                .then()
                .statusCode(200)
                .body(equalTo("alice, required_claim:1,2"));

        RestAssured.when().post("/oidc/opaque-token-3-call-count").then().body(equalTo("0"));
    }

    @Test
    public void testResolveStaticTenantsByPathPatterns() {
        // default tenant path pattern is more specific, therefore it wins over tenant-b pattern that is also matched
        assertStaticTenantSuccess("a", "default", "tenant-b/default");
        assertStaticTenantFailure("a", "tenant-b/default-b");
        assertStaticTenantFailure("a", "tenant-b/default-b/");
        assertStaticTenantFailure("b", "tenant-b/default");
        assertStaticTenantFailure("b", "tenant-b/default/");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b/");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b/default-b");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b/default-b/");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b/public-key");
        assertStaticTenantSuccess("b", "tenant-b", "tenant-b/public-key");
        assertStaticTenantFailure("public-key", "tenant-b/public-key");
        assertStaticTenantFailure("public-key", "tenant-b/public-key/");
        assertStaticTenantSuccess("public-key", "public-key", "tenant-c/public-key");
        assertStaticTenantSuccess("public-key", "public-key", "tenant-c/public-key/");
        assertStaticTenantSuccess("public-key", "public-key", "public-key/match");
        assertStaticTenantSuccess("public-key", "public-key", "public-key/match/");
        assertStaticTenantFailure("b", "public-key/match");
        assertStaticTenantFailure("b", "public-key/match/");
        assertStaticTenantFailure("public-key", "public-key-c/match");
        assertStaticTenantFailure("public-key", "public-key-c/match/");
        assertStaticTenantSuccess("a", "public-key", "public-key-c/match");

        // assert path is normalized and tenant is selected by path-matching pattern before HTTP perms are checked
        Vertx vertx = Vertx.vertx();
        HttpClient httpClient = null;
        try {
            httpClient = vertx.createHttpClient();
            httpClient
                    .request(HttpMethod.GET, RestAssured.port, URI.create(RestAssured.baseURI).getHost(),
                            "/api/tenant-paths///public-key//match")
                    .flatMap(r -> r.putHeader("Authorization", "Bearer " + getAccessToken("public-key")).send())
                    .flatMap(r -> {
                        assertEquals(200, r.statusCode());
                        return r.body();
                    })
                    .map(Buffer::toString)
                    .invoke(b -> assertEquals("public-key", b))
                    .await().indefinitely();
            httpClient
                    .request(HttpMethod.GET, RestAssured.port, URI.create(RestAssured.baseURI).getHost(),
                            "/api/tenant-paths///public-key//match")
                    .flatMap(r -> r.putHeader("Authorization", "Bearer " + getAccessToken("b")).send())
                    .invoke(r -> assertEquals(401, r.statusCode()))
                    .await().indefinitely();
        } finally {
            if (httpClient != null) {
                httpClient.closeAndAwait();
            }
            vertx.closeAndAwait();
        }
    }

    @Test
    public void testResolveTenantsByIssuer() {
        assertStaticTenantSuccess("e", "tenant-e", "tenant-by-issuer");
        assertStaticTenantSuccess("f", "tenant-f", "tenant-by-issuer");
    }

    private void assertStaticTenantSuccess(String clientId, String tenant, String subPath) {
        // tenant is resolved based on path pattern and access token is valid
        final String accessToken = getAccessToken(clientId);
        RestAssured.given().auth().oauth2(accessToken).when().get("/api/tenant-paths/" + subPath).then().statusCode(200)
                .body(equalTo(tenant));
    }

    private String getAccessToken(String clientId) {
        final String accessToken;
        if ("public-key".equals(clientId)) {
            accessToken = AnnotationBasedTenantTest.getTokenWithRole();
        } else {
            accessToken = getAccessToken("alice", clientId);
        }
        return accessToken;
    }

    private void assertStaticTenantFailure(String clientId, String subPath) {
        // tenant is not resolved based on path pattern or access token is not valid
        final String accessToken = getAccessToken(clientId);
        RestAssured.given().auth().oauth2(accessToken).when().get("/api/tenant-paths/" + subPath).then().statusCode(401);
    }

    @Test
    public void testAnnotationBasedAuthMechSelection() throws IOException {
        // endpoint is annotated with @CodeFlow
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/tenant/tenant-web-app-dynamic/api/user/code-flow-auth-mech-annotation");
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("1"))
                .when().get("/tenant/tenant-oidc-no-discovery/api/user/code-flow-auth-mech-annotation")
                .then()
                .statusCode(401);

        // endpoint is annotated with @Bearer
        RestAssured.given().auth().oauth2(getAccessTokenFromSimpleOidc("1"))
                .when().get("/tenant/tenant-oidc-no-discovery/api/user/bearer-auth-mech-annotation")
                .then()
                .statusCode(204); // ID token name is null
        boolean codeFlowAuthFailed = false;
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/tenant/tenant-web-app-dynamic/api/user/bearer-auth-mech-annotation");
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            webClient.getOptions().setRedirectEnabled(false);
            loginForm.getButtonByName("login").click();
        } catch (FailingHttpStatusCodeException e) {
            codeFlowAuthFailed = true;
        }
        if (!codeFlowAuthFailed) {
            Assertions.fail(
                    "Endpoint 'bearer-auth-mech-annotation' is annotated with the @Bearer annotation, code flow auth should fail");
        }
    }

    @Test
    public void testStepUpAuthUsingRequiredClaimsConfigProperty() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/revoke-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/disable-discovery").then().body(equalTo("false"));

        // for this tenant, 3 'acr' values are required: alpha, beta, gamma
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of()))
                .when().get("/tenants/step-up-auth-required-claims/api/user")
                .then()
                .statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("alpha"))
                .header("www-authenticate", containsString("beta"))
                .header("www-authenticate", containsString("gamma"));
        // 'gamma' acr value is missing -> 401
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of("alpha", "beta")))
                .when().get("/tenants/step-up-auth-required-claims/api/user")
                .then()
                .statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("alpha"))
                .header("www-authenticate", containsString("beta"))
                .header("www-authenticate", containsString("gamma"));
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of("alpha", "beta", "gamma")))
                .when().get("/tenants/step-up-auth-required-claims/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice:service"));
    }

    @Test
    public void testStepUpAuthUsingCustomValidatorRequiringAcrValues() {
        RestAssured.when().post("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/introspection-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/revoke-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().post("/oidc/disable-discovery").then().body(equalTo("false"));

        // for this tenant, 3 'acr' values are required: delta, epsilon, zeta
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of()))
                .when().get("/tenants/step-up-auth-custom-validator/api/user")
                .then()
                .statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("delta"))
                .header("www-authenticate", containsString("epsilon"))
                .header("www-authenticate", containsString("zeta"));
        // 'gamma' acr value is missing -> 401
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of("alpha", "beta")))
                .when().get("/tenants/step-up-auth-custom-validator/api/user")
                .then()
                .statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("delta"))
                .header("www-authenticate", containsString("epsilon"))
                .header("www-authenticate", containsString("zeta"));
        RestAssured.given().auth().oauth2(getAccessTokenWithAcr(Set.of("delta", "epsilon", "zeta")))
                .when().get("/tenants/step-up-auth-custom-validator/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice:service"));
    }

    @Test
    void testBearerTokenAuthenticationRequestFilter() {
        // reset current state
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/oidc/opaque-token-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/opaque-token-3-call-count").then().body(equalTo("0"));
        RestAssured.given().get("/oidc-filter/request/custom-bearer-token-auth").then().statusCode(200);
        RestAssured.given().get("/oidc-filter/response/custom-tenant-feature-auth").then().statusCode(200);

        String opaqueToken2 = getOpaqueAccessToken2FromSimpleOidc();
        RestAssured.given().auth().oauth2(opaqueToken2)
                .when().get("/tenant-introspection/tenant-introspection-required-claims")
                .then()
                .statusCode(200)
                .body(equalTo("alice, required_claim:1"));

        // assert and reset current state
        RestAssured.given().get("/oidc-filter/request/custom-bearer-token-auth").then().statusCode(200)
                .body(is("tenant-introspection-required-claims"));
        RestAssured.given().get("/oidc-filter/response/custom-tenant-feature-auth").then().statusCode(200)
                .body(is(""));

        String opaqueToken3 = getOpaqueAccessToken3FromSimpleOidc();
        RestAssured.given().auth().oauth2(opaqueToken3)
                .when().get("/tenant-introspection/tenant-introspection-multiple-required-claims")
                .then()
                // introspection called fails
                .statusCode(401);

        // assert and reset current state
        RestAssured.given().get("/oidc-filter/request/custom-bearer-token-auth").then().statusCode(200)
                .body(is("tenant-introspection-multiple-required-claims"));
        RestAssured.given().get("/oidc-filter/response/custom-tenant-feature-auth").then().statusCode(200)
                .body(is("tenant-introspection-multiple-required-claims"));

        // reset state
        RestAssured.when().post("/oidc/enable-introspection").then().body(equalTo("true"));
        RestAssured.when().post("/oidc/opaque-token-call-count").then().body(equalTo("0"));
        RestAssured.when().post("/oidc/opaque-token-3-call-count").then().body(equalTo("0"));
    }

    private String getAccessToken(String userName, String clientId) {
        return getAccessToken(userName, clientId, clientId);
    }

    private String getAccessToken(String userName, String realmId, String clientId) {
        return getAccessToken(userName, realmId, clientId, null);
    }

    private String getAccessToken(String userName, String realmId, String clientId, List<String> scopes) {
        return client.getRealmAccessToken("quarkus-" + realmId, userName, userName, "quarkus-app-" + clientId, "secret",
                scopes);
    }

    private String getAccessTokenFromSimpleOidc(String kid) {
        return getAccessTokenFromSimpleOidc("123456789", kid);
    }

    private String getAccessTokenFromSimpleOidc(String subject, String kid) {
        return getAccessTokenFromSimpleOidc(subject, kid, "/oidc/accesstoken");
    }

    private String getAccessTokenWithEmptyScopeFromSimpleOidc(String kid) {
        return getAccessTokenFromSimpleOidc("123456789", kid, "/oidc/accesstoken-empty-scope");
    }

    private static String getAccessTokenFromSimpleOidc(String subject, String kid, String tokenEndpoint) {
        String json = RestAssured
                .given()
                .queryParam("sub", subject)
                .queryParam("kid", kid)
                .formParam("grant_type", "authorization_code")
                .when()
                .post(tokenEndpoint)
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

    private String getOpaqueAccessTokenFromSimpleOidc() {
        String json = RestAssured
                .when()
                .post("/oidc/opaque-token")
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

    private String getOpaqueAccessToken2FromSimpleOidc() {
        String json = RestAssured
                .when()
                .post("/oidc/opaque-token2")
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

    private String getOpaqueAccessToken3FromSimpleOidc() {
        String json = RestAssured
                .when()
                .post("/oidc/opaque-token3")
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

    static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getStateCookie(WebClient webClient, String tenantId) {
        String cookieSuffix = "q_auth" + (tenantId == null ? "" : "_" + tenantId) + "_";
        for (Cookie c : webClient.getCookieManager().getCookies()) {
            if (c.getName().startsWith(cookieSuffix) && c.getName().length() > cookieSuffix.length()) {
                return c;
            }
        }
        return null;
    }

    static Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }

    private String getStateCookieSavedPath(WebClient webClient, String tenantId) {
        String[] parts = getStateCookie(webClient, tenantId).getValue().split("\\|");
        return parts.length == 2 ? parts[1] : null;
    }

    private Cookie getSessionAtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_at" + (tenantId == null ? "_Default_test" : "_" + tenantId));
    }

    private Cookie getSessionRtCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_rt" + (tenantId == null ? "_Default_test" : "_" + tenantId));
    }
}
