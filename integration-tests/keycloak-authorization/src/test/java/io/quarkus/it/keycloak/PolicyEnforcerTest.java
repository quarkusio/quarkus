package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class PolicyEnforcerTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
    }

    @AfterAll
    public static void removeKeycloakRealm() {
    }

    @Test
    public void testUserHasAdminRoleServiceTenant() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource Tenant"));
    }

    @Test
    public void testUserHasSuperUserRoleWebTenant() throws Exception {
        testWebAppTenantAllowed("alice");
        testWebAppTenantForbidden("admin");
        testWebAppTenantForbidden("jdoe");
    }

    private void testWebAppTenantAllowed(String user) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/api-permission-webapp");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute(user);
            loginForm.getInputByName("password").setValueAttribute(user);
            WebResponse response = loginForm.getInputByName("login").click().getWebResponse();
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContentAsString().contains("Permission Resource WebApp"));
            webClient.getCookieManager().clearCookies();
        }
    }

    private void testWebAppTenantForbidden(String user) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/api-permission-webapp");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute(user);
            loginForm.getInputByName("password").setValueAttribute(user);
            try {
                loginForm.getInputByName("login").click();
                fail("403 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(403, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    @Test
    public void testUserHasRoleConfidential() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission/scope?scope=write")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission/scope?scope=read")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("read"));

        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);

        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/permission/entitlements")
                .then()
                .statusCode(200);
    }

    @Test
    public void testRequestParameterAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=true")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Claim Protected Resource"));
        ;
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=false")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testHttpResponseFromExternalServiceAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Http Response Claim Protected Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testBodyClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .contentType(ContentType.JSON)
                .body("{\"from-body\": \"grant\"}")
                .when()
                .post("/api/permission/body-claim")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Body Claim Protected Resource"));
    }

    @Test
    public void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public")
                .then()
                .statusCode(204);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicy() {
        RestAssured.given()
                .when().get("/api/public-enforcing")
                .then()
                .statusCode(401);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicyAndToken() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/public-enforcing")
                .then()
                .statusCode(403);
    }

    @Test
    public void testPublicResourceWithDisabledPolicyAndToken() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/public-token")
                .then()
                .statusCode(204);
    }

    @Test
    public void testPathConfigurationPrecedenceWhenPathCacheNotDefined() {
        RestAssured.given()
                .when().get("/api2/resource")
                .then()
                .statusCode(401);

        RestAssured.given()
                .when().get("/hello")
                .then()
                .statusCode(404);

        RestAssured.given()
                .when().get("/")
                .then()
                .statusCode(404);
    }

    private String getAccessToken(String userName) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }
}
