package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
@QuarkusTestResource(KeycloakLifecycleManager.class)
public class PolicyEnforcerTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String KEYCLOAK_REALM = "quarkus";

    @TestHTTPResource
    URL url;

    static Vertx vertx = Vertx.vertx();
    static WebClient client = WebClient.create(vertx);

    @AfterAll
    public static void closeVertxClient() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
            vertx = null;
        }
    }

    @Test
    public void testUserHasAdminRoleServiceTenant() {
        assureGetPath("/api-permission-tenant", 403, getAccessToken("alice"), null);
        assureGetPath("//api-permission-tenant", 403, getAccessToken("alice"), null);

        assureGetPath("/api-permission-tenant", 403, getAccessToken("jdoe"), null);
        assureGetPath("//api-permission-tenant", 403, getAccessToken("jdoe"), null);

        assureGetPath("/api-permission-tenant", 200, getAccessToken("admin"), "Permission Resource Tenant");
        assureGetPath("//api-permission-tenant", 200, getAccessToken("admin"), "Permission Resource Tenant");
    }

    @Test
    public void testUserHasSuperUserRoleWebTenant() throws Exception {
        testWebAppTenantAllowed("alice");
        testWebAppTenantForbidden("admin");
        testWebAppTenantForbidden("jdoe");
    }

    private void testWebAppTenantAllowed(String user) throws Exception {
        try (final com.gargoylesoftware.htmlunit.WebClient webClient = createWebClient()) {
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
        try (final com.gargoylesoftware.htmlunit.WebClient webClient = createWebClient()) {
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

    private com.gargoylesoftware.htmlunit.WebClient createWebClient() {
        com.gargoylesoftware.htmlunit.WebClient webClient = new com.gargoylesoftware.htmlunit.WebClient();
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
        assureGetPath("/api/permission", 403, getAccessToken("alice"), null);
        assureGetPath("//api/permission", 403, getAccessToken("alice"), null);

        assureGetPath("/api/permission", 200, getAccessToken("jdoe"), "Permission Resource");
        assureGetPath("//api/permission", 200, getAccessToken("jdoe"), "Permission Resource");

        assureGetPath("/api/permission/scope?scope=write", 403, getAccessToken("jdoe"), null);
        assureGetPath("//api/permission/scope?scope=write", 403, getAccessToken("jdoe"), null);

        assureGetPath("/api/permission/scope?scope=read", 200, getAccessToken("jdoe"), "read");
        assureGetPath("//api/permission/scope?scope=read", 200, getAccessToken("jdoe"), "read");

        assureGetPath("/api/permission", 403, getAccessToken("admin"), null);
        assureGetPath("//api/permission", 403, getAccessToken("admin"), null);

        assureGetPath("/api/permission/entitlements", 200, getAccessToken("admin"), null);
        assureGetPath("//api/permission/entitlements", 200, getAccessToken("admin"), null);
    }

    @Test
    public void testRequestParameterAsClaim() {
        assureGetPath("/api/permission/claim-protected?grant=true", 200, getAccessToken("alice"),
                "Claim Protected Resource");
        assureGetPath("//api/permission/claim-protected?grant=true", 200, getAccessToken("alice"),
                "Claim Protected Resource");

        assureGetPath("/api/permission/claim-protected?grant=false", 403, getAccessToken("alice"), null);
        assureGetPath("//api/permission/claim-protected?grant=false", 403, getAccessToken("alice"), null);

        assureGetPath("/api/permission/claim-protected", 403, getAccessToken("alice"), null);
        assureGetPath("//api/permission/claim-protected", 403, getAccessToken("alice"), null);
    }

    @Test
    public void testHttpResponseFromExternalServiceAsClaim() {
        assureGetPath("/api/permission/http-response-claim-protected", 200, getAccessToken("alice"), null);
        assureGetPath("//api/permission/http-response-claim-protected", 200, getAccessToken("alice"), null);

        assureGetPath("/api/permission/http-response-claim-protected", 403, getAccessToken("jdoe"), null);
        assureGetPath("//api/permission/http-response-claim-protected", 403, getAccessToken("jdoe"), null);
    }

    @Test
    public void testBodyClaim() {
        assurePostPath("/api/permission/body-claim", "{\"from-body\": \"grant\"}", 200, getAccessToken("alice"),
                "Body Claim Protected Resource");
    }

    @Test
    public void testPublicResource() {
        assureGetPath("/api/public", 204, null, null);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicy() {
        assureGetPath("/api/public-enforcing", 401, null, null);
        assureGetPath("//api/public-enforcing", 401, null, null);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicyAndToken() {
        assureGetPath("/api/public-enforcing", 403, getAccessToken("alice"), null);
        assureGetPath("//api/public-enforcing", 403, getAccessToken("alice"), null);
    }

    @Test
    public void testPublicResourceWithDisabledPolicyAndToken() {
        assureGetPath("/api/public-token", 204, getAccessToken("alice"), null);
    }

    @Test
    public void testPathConfigurationPrecedenceWhenPathCacheNotDefined() {
        assureGetPath("/api2/resource", 401, null, null);
        assureGetPath("//api2/resource", 401, null, null);

        assureGetPath("/hello", 404, null, null);
        assureGetPath("//hello", 404, null, null);

        assureGetPath("/", 404, null, null);
        assureGetPath("//", 400, null, null);
    }

    protected String getAccessToken(String userName) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .when()
                .post(KeycloakLifecycleManager.KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM
                        + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private void assureGetPath(String path, int expectedStatusCode, String token, String body) {
        var req = client.get(url.getPort(), url.getHost(), path);
        if (token != null) {
            req.bearerTokenAuthentication(token);
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
        if (body != null) {
            assertTrue(result.result().bodyAsString().contains(body), path);
        }
    }

    private void assurePostPath(String path, String requestBody, int expectedStatusCode, String token,
            String responseBody) {
        var req = client.post(url.getPort(), url.getHost(), path);
        if (token != null) {
            req.bearerTokenAuthentication(token);
        }
        req.putHeader("Content-Type", "application/json");
        var result = req.sendJson(new JsonObject(requestBody));
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
        if (responseBody != null) {
            assertTrue(result.result().bodyAsString().contains(responseBody), path);
        }
    }

}
