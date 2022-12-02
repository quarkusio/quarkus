package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("alice", textPage.getContent());
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
