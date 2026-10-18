package io.quarkus.it.oidc.dev.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(CodeFlowOidcDevServicesTest.OidcWebAppTestProfile.class)
public class CodeFlowOidcDevServicesTest {

    @Test
    public void testLoginAsCustomUser() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/admin-only");

            Assertions.assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "custom-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            loginForm.getInputByName("name").setValueAttribute("Ronald");
            loginForm.getInputByName("roles").setValueAttribute("admin,user");

            TextPage adminOnlyPage = loginForm.getButtonByName("login").click();
            Assertions.assertTrue(adminOnlyPage.getContent().contains("Ronald"));
            Assertions.assertTrue(adminOnlyPage.getContent().contains("admin"));
            Assertions.assertTrue(adminOnlyPage.getContent().contains("user"));

            assertNotNull(webClient.getCookieManager().getCookie("q_session"));

            testLogout(webClient);
        }
    }

    @Test
    public void testLoginAsAlice() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/admin-only");

            Assertions.assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "predefined-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            TextPage adminOnlyPage = loginForm.getButtonByName("predefined-alice").click();
            Assertions.assertTrue(adminOnlyPage.getContent().contains("alice"));
            Assertions.assertTrue(adminOnlyPage.getContent().contains("admin"));
            Assertions.assertTrue(adminOnlyPage.getContent().contains("user"));

            testLogout(webClient);
        }
    }

    @Test
    public void testLoginAsBob() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/user-only");

            Assertions.assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "predefined-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            TextPage adminOnlyPage = loginForm.getButtonByName("predefined-bob").click();
            Assertions.assertTrue(adminOnlyPage.getContent().contains("bob"));
            Assertions.assertTrue(adminOnlyPage.getContent().contains("user"));
            Assertions.assertFalse(adminOnlyPage.getContent().contains("admin"));

            try {
                webClient.getPage("http://localhost:8081/secured/admin-only");
                fail("Exception is expected because Bob is not admin");
            } catch (FailingHttpStatusCodeException ex) {
                Assertions.assertEquals(403, ex.getStatusCode());
            }

            testLogout(webClient);
        }
    }

    private static void testLogout(WebClient webClient) throws IOException {
        webClient.getOptions().setRedirectEnabled(false);
        WebResponse webResponse = webClient
                .loadWebResponse(new WebRequest(URI.create(getAuthServerUrl()
                        + "/logout?post_logout_redirect_uri=north-pole&id_token_hint=SECRET")
                        .toURL()));
        Assertions.assertEquals(302, webResponse.getStatusCode());
        Assertions.assertEquals("north-pole", webResponse.getResponseHeaderValue("Location"));

        webClient.getCookieManager().clearCookies();
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static String getAuthServerUrl() {
        return RestAssured.get("/secured/auth-server-url").then().statusCode(200).extract().body().asString();
    }

    public static class OidcWebAppTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "code-flow";
        }
    }

}
