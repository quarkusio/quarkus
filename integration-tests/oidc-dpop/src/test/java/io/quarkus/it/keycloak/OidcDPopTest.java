package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class OidcDPopTest {

    @Test
    public void testDPopProof() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("Hello, alice; JWK thumbprint in JWT: true, JWK thumbprint in introspection: false",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();

            checkHealth();
        }
    }

    private static void checkHealth() {
        Response healthReadyResponse = RestAssured.when().get("http://localhost:8081/q/health/ready");
        JsonObject jsonHealth = new JsonObject(healthReadyResponse.asString());
        JsonObject oidcCheck = jsonHealth.getJsonArray("checks").getJsonObject(0);
        assertEquals("UP", oidcCheck.getString("status"));
        assertEquals("OIDC Provider Health Check", oidcCheck.getString("name"));

        JsonObject data = oidcCheck.getJsonObject("data");
        assertEquals("Disabled", data.getString("Default Tenant"));
        assertEquals("OK", data.getString("DPoP Tenant"));
    }

    @Test
    public void testDPopProofWrongHttpMethod() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt-wrong-dpop-http-method");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWrongHttpUri() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt-wrong-dpop-http-uri");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWrongSignature() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt-wrong-dpop-signature");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWrongJwkKey() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt-wrong-dpop-jwk-key");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWrongTokenHash() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt-wrong-dpop-token-hash");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

}
