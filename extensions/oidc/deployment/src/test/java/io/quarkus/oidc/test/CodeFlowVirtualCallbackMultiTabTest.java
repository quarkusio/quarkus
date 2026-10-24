package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

/**
 * The redirect path has no resource behind it: the code flow callback must be handled by the authentication
 * mechanism whether or not a session already exists, see <a href="https://github.com/quarkusio/quarkus/issues/35391">
 * GitHub issue #35391</a>.
 */
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowVirtualCallbackMultiTabTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResource.class)
                    .addAsResource("application-virtual-callback.properties", "application.properties"));

    @Test
    public void testSingleTab() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = login(webClient.getPage("http://localhost:8081/protected"));
            assertEquals("http://localhost:8081/protected", page.getUrl().toString());
            assertEquals("alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSecondTabCompletesAfterTheFirstOneLoggedIn() throws Exception {
        // one WebClient is one browser: both tabs share the cookies
        try (final WebClient webClient = createWebClient()) {
            // the second tab reaches the login page and stays there
            HtmlPage secondTabLoginPage = webClient.getPage("http://localhost:8081/protected");
            assertEquals("Sign in to quarkus", secondTabLoginPage.getTitleText());
            String secondTabAuthorizationUrl = secondTabLoginPage.getUrl().toString();

            // the first tab logs in
            HtmlPage firstTab = login(webClient.getPage("http://localhost:8081/protected"));
            assertEquals("alice", firstTab.getBody().asNormalizedText());

            // the second tab completes its own code flow: the provider has a session for the user, so its
            // authorization endpoint redirects straight back with a code for the second tab's state
            HtmlPage secondTab = webClient.getPage(secondTabAuthorizationUrl);
            assertEquals("http://localhost:8081/protected", secondTab.getUrl().toString());
            assertEquals("alice", secondTab.getBody().asNormalizedText());

            // and the session keeps working
            HtmlPage page = webClient.getPage("http://localhost:8081/protected");
            assertEquals("alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
        }
    }

    private static HtmlPage login(HtmlPage loginPage) throws Exception {
        assertEquals("Sign in to quarkus", loginPage.getTitleText());
        HtmlForm loginForm = loginPage.getForms().get(0);
        loginForm.getInputByName("username").setValueAttribute("alice");
        loginForm.getInputByName("password").setValueAttribute("alice");
        return loginForm.getButtonByName("login").click();
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
