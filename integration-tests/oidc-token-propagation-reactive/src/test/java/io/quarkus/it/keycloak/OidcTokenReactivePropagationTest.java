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

@QuarkusTest
public class OidcTokenReactivePropagationTest {

    @Test
    public void testGetUserNameWithAccessTokenPropagation() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/frontend/access-token-propagation");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();
            assertEquals("Bearer:alice", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/frontend/id-token-propagation");
            assertEquals("ID:alice", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testNoToken() {
        RestAssured.given().when().redirects().follow(false)
                .get("/frontend/access-token-propagation")
                .then()
                .statusCode(302);
    }

    private WebClient createWebClient() throws Exception {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    @Test
    public void testGetUserNameWithServiceWithoutToken() {
        RestAssured.when().get("/frontend/service-without-token")
                .then()
                .statusCode(500);
    }

}
