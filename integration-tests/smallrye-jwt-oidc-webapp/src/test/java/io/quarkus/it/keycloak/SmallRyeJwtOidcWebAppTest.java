package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

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
            assertTokenStateCount(0);
            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
            assertTokenStateCount(1);
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static void assertTokenStateCount(Integer expectedNumOfTokens) {
        RestAssured
                .get("/public/token-state-count")
                .then()
                .statusCode(200)
                .body(Matchers.is(expectedNumOfTokens.toString()));
    }
}
