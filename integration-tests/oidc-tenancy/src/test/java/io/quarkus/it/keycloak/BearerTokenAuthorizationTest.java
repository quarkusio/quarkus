package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class BearerTokenAuthorizationTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus-";

    @Test
    public void testResolveTenantIdentifierWebApp() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testResolveTenantIdentifierWebApp2() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user/webapp2");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Sign in to quarkus-webapp2", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app2:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testHybridWebApp() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenants/tenant-hybrid/api/user");
            assertNotNull(getStateCookie(webClient, "tenant-hybrid-webapp"));
            assertEquals("Sign in to quarkus-hybrid", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("alice:web-app", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
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
            page = loginForm.getInputByName("login").click();
            assertEquals("alice:web-app", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
        RestAssured.given().auth().oauth2(getAccessToken("alice", "hybrid"))
                .when().get("/tenants/tenant-hybrid-webapp-service/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("alice:service"));
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
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app-no-discovery:alice", page.getBody().asText());

            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app-no-discovery/api/user/webapp-no-discovery");
            assertEquals("tenant-web-app-no-discovery:alice", page.getBody().asText());
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
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app:alice", page.getBody().asText());
            // tenant-web-app2
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user/webapp2");
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Sign in to quarkus-webapp2", page.getTitleText());
            loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app2:alice", page.getBody().asText());

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
        RestAssured.given().auth().oauth2(getAccessToken("alice", "d"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-d:alice.alice"));

        // should give a 401 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testResolveTenantConfigNoDiscovery() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
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
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
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
                .when().get("/tenant/tenant-oidc/api/user")
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
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
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
                    .body(equalTo("tenant-oidc-introspection-only:alice:0"));
        }

        RestAssured.when().get("/oidc/jwk-endpoint-call-count").then().body(equalTo("0"));
        RestAssured.when().get("/oidc/introspection-endpoint-call-count").then().body(equalTo("3"));
        RestAssured.when().post("/oidc/disable-introspection").then().body(equalTo("false"));
        RestAssured.when().get("/oidc/userinfo-endpoint-call-count").then().body(equalTo("3"));
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
                    .body(equalTo("tenant-oidc-introspection-only-cache:alice:" + expectedCacheSize));
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
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app-dynamic:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    private String getAccessToken(String userName, String clientId) {
        return getAccessToken(userName, clientId, clientId);
    }

    private String getAccessToken(String userName, String realmId, String clientId) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app-" + clientId)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + realmId + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private String getAccessTokenFromSimpleOidc(String kid) {
        String json = RestAssured
                .given()
                .queryParam("kid", kid)
                .when()
                .post("/oidc/token")
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

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private Cookie getStateCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_auth" + (tenantId == null ? "" : "_" + tenantId));
    }

    private String getStateCookieSavedPath(WebClient webClient, String tenantId) {
        String[] parts = getStateCookie(webClient, tenantId).getValue().split("\\|");
        return parts.length == 2 ? parts[1] : null;
    }
}
