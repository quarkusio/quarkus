package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OidcRichAuthorizationRequestsTest {

    @Test
    void testAuthorizationDetails() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/rar/token-response/authorization-details");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            String authorizationDetailsFromTokenResponse = textPage.getContent().trim();
            assertNotNull(authorizationDetailsFromTokenResponse);
            assertTrue(authorizationDetailsFromTokenResponse.contains("type=openid_credential"),
                    authorizationDetailsFromTokenResponse);
            assertTrue(authorizationDetailsFromTokenResponse.contains("credential_configuration_id=vc-scope-mapping"),
                    authorizationDetailsFromTokenResponse);
            assertTrue(authorizationDetailsFromTokenResponse.contains("credential_identifiers"),
                    authorizationDetailsFromTokenResponse);

            webClient.getCookieManager().clearCookies();
        }
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
