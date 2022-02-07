package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class SmallRyeJwtOidcWebAppTest {

    @Test
    public void testGetUserNameWithBearerToken() {
        RestAssured.given().auth().oauth2(KeycloakRealmResourceManager.getAccessToken("alice"))
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameWithWrongBearerToken() {
        RestAssured.given().auth().oauth2("123")
                .when().get("/protected")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGetUserNameWithCookieToken() {
        RestAssured.given().header("Cookie", "Bearer=" + KeycloakRealmResourceManager.getAccessToken("alice"))
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameWithWrongCookieToken() {
        RestAssured.given().header("Cookie", "Bearer=123")
                .when().get("/protected")
                .then()
                .statusCode(401);
    }

    @Test
    public void testNoToken() {
        // OIDC has a higher priority than JWT
        RestAssured.given().when().redirects().follow(false)
                .get("/protected")
                .then()
                .statusCode(302);
    }

    @Test
    public void testGetUserNameWithCodeFlow() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
