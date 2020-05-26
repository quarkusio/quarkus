package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));
            assertEquals("Log in to quarkus-webapp", page.getTitleText());
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
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user");
            // State cookie is available but there must be no saved path parameter
            // as the tenant-web-app configuration does not set a redirect-path property
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Log in to quarkus-webapp2", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app2:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testReAuthenticateWhenSwitchingTenants() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            // tenant-web-app
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user");
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app"));
            assertEquals("Log in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app:alice", page.getBody().asText());
            // tenant-web-app2
            page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app2/api/user");
            assertNull(getStateCookieSavedPath(webClient, "tenant-web-app2"));
            assertEquals("Log in to quarkus-webapp2", page.getTitleText());
            loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getInputByName("login").click();
            assertEquals("tenant-web-app2:alice", page.getBody().asText());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testResolveTenantIdentifier() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-b/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-b:alice"));

        // should give a 403 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-c/api/user")
                .then()
                .statusCode(403);
    }

    @Test
    public void testResolveTenantConfig() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "d"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(200)
                .body(equalTo("tenant-d:alice"));

        // should give a 403 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(403);
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

    private String getAccessToken(String userName, String clientId) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app-" + clientId)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + clientId + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
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
