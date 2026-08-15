package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class OidcDPopTest {

    @Test
    public void testDPopProof() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            TextPage textPage = loginAndClick(webClient, "login-jwt");

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
        assertInvalidDPoPProof("login-jwt-wrong-dpop-http-method");
    }

    @Test
    public void testDPopProofWrongHttpUri() throws Exception {
        assertInvalidDPoPProof("login-jwt-wrong-dpop-http-uri");
    }

    @Test
    public void testDPopProofWrongSignature() throws Exception {
        assertInvalidDPoPProof("login-jwt-wrong-dpop-signature");
    }

    @Test
    public void testDPopProofWrongJwkKey() throws Exception {
        assertInvalidDPoPProof("login-jwt-wrong-dpop-jwk-key");
    }

    @Test
    public void testDPopProofWrongTokenHash() throws Exception {
        assertInvalidDPoPProof("login-jwt-wrong-dpop-token-hash");
    }

    private void assertInvalidDPoPProof(String loginPath) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            resetDPoPAuthFailureObserver();
            TextPage textPage = loginAndClick(webClient, loginPath);

            assertEquals("401 status from ProtectedResource", textPage.getContent());
            assertInvalidDPoPProofAuthFailureEvent();

            webClient.getCookieManager().clearCookies();
        }
    }

    static TextPage loginAndClick(WebClient webClient, String loginPath) throws Exception {
        HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/" + loginPath);
        assertEquals("Sign in to quarkus", page.getTitleText());
        HtmlForm loginForm = page.getForms().get(0);
        loginForm.getInputByName("username").setValueAttribute("alice");
        loginForm.getInputByName("password").setValueAttribute("alice");
        return loginForm.getButtonByName("login").click();
    }

    static void resetDPoPAuthFailureObserver() {
        RestAssured.delete("http://localhost:8081/dpop-auth-failure/last-error");
    }

    static void assertInvalidDPoPProofAuthFailureEvent() {
        RestAssured.get("http://localhost:8081/dpop-auth-failure/last-error")
                .then().statusCode(200).body(Matchers.equalTo(OidcConstants.INVALID_DPOP_PROOF));
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

}
