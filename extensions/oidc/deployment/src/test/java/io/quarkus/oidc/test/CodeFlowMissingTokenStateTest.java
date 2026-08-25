package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowMissingTokenStateTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResource.class, MissingTokenStateManager.class,
                            MissingTokenStateResource.class)
                    .addAsResource("application-missing-token-state.properties", "application.properties"));

    @Test
    public void testTokenStateManagerReturnsNullTokens() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());
            assertTrue(hasSessionCookie(webClient));

            // the token state is no longer available, the user must be redirected to re-authenticate
            TextPage textPage = webClient.getPage("http://localhost:8081/lose-token-state");
            assertEquals("token state lost", textPage.getContent());

            webClient.getOptions().setRedirectEnabled(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            textPage = webClient.getPage("http://localhost:8081/protected");
            assertEquals(302, textPage.getWebResponse().getStatusCode());
            assertFalse(hasSessionCookie(webClient));

            webClient.getCookieManager().clearCookies();
        }
    }

    private static boolean hasSessionCookie(WebClient webClient) {
        return webClient.getCookieManager().getCookies().stream().anyMatch(c -> c.getName().startsWith("q_session"));
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
