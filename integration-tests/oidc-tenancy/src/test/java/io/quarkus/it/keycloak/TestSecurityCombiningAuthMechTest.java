package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.AnnotationBasedTenantTest.getTokenWithRole;
import static io.quarkus.it.keycloak.BearerTokenAuthorizationTest.createWebClient;
import static io.quarkus.it.keycloak.BearerTokenAuthorizationTest.getSessionCookie;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class TestSecurityCombiningAuthMechTest {

    @TestHTTPEndpoint(MultipleAuthMechResource.class)
    @TestSecurity(user = "testUser", authMechanism = "basic")
    @Test
    public void testBasicAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/annotation")
                .then()
                .statusCode(401);
    }

    @TestHTTPEndpoint(MultipleAuthMechResource.class)
    @TestSecurity(user = "testUser", authMechanism = "Bearer")
    @Test
    public void testBearerBasedAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("bearer/policy")
                .then()
                .statusCode(200);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("bearer/annotation")
                .then()
                .statusCode(200);
    }

    @TestHTTPEndpoint(MultipleAuthMechResource.class)
    @TestSecurity(user = "testUser", authMechanism = "custom")
    @Test
    public void testCustomAuthentication() {
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/policy")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .get("basic/annotation")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .redirects().follow(false)
                .get("bearer/annotation")
                .then()
                .statusCode(401);
    }

    @TestHTTPEndpoint(TenantEchoResource.class)
    @TestSecurity(user = "testUser", authMechanism = "Bearer", roles = "role1")
    @Test
    public void testHttpCredentialsHasPriorityOverTestSecurity() {
        // token has priority over @TestSecurity
        RestAssured.given().auth().oauth2(getTokenWithRole("role1"))
                .when().get("jax-rs-perm-check")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=tenant-public-key, name=alice")));
        // no token -> use @TestSecurity
        RestAssured.given()
                .when().get("jax-rs-perm-check")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=null, name=testUser")));
    }

    @TestSecurity(user = "testUser", authMechanism = "Bearer", roles = "role1")
    @Test
    public void testSessionCookieHasPriorityOverTestSecurity() throws IOException {
        // @TestSecurity still use Bearer authentication as we didn't specify credentials
        RestAssured.given()
                .redirects().follow(false)
                .when().get("/tenant/tenant-web-app/api/user/webapp-local-logout")
                .then().statusCode(302);
        RestAssured.given()
                .when().get("/api/tenant-echo/jax-rs-perm-check")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=null, name=testUser")));

        // path specific authentication is still possible, the @TestSecurity is not used as it uses Bearer, not code
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant/tenant-web-app/api/user/webapp-local-logout");
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("alice", page.getBody().asNormalizedText());
            assertNull(getSessionCookie(webClient, "tenant-web-app"));
        }
    }
}
