package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowDevModeDefaultTenantTestCase {

    private static Class<?>[] testClasses = {
            ProtectedResource.class,
            UnprotectedResource.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-dev-mode-default-tenant.properties", "application.properties"));

    @Test
    public void testAccessAndRefreshTokenInjectionDevMode() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {

            try {
                webClient.getPage("http://localhost:8080/protected");
                fail("Exception is expected because by default the bearer token is required");
            } catch (FailingHttpStatusCodeException ex) {
                // Reported by Quarkus
                assertEquals(401, ex.getStatusCode());
            }

            // Enable 'web-app' application type
            test.modifyResourceFile("application.properties",
                    s -> s.replace("#quarkus.oidc.application-type=web-app", "quarkus.oidc.application-type=web-app"));

            HtmlPage page = webClient.getPage("http://localhost:8080/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();

            // Enable the invalid scope
            test.modifyResourceFile("application.properties",
                    s -> s.replace("#quarkus.oidc.authentication.scopes=invalid-scope",
                            "quarkus.oidc.authentication.scopes=invalid-scope"));

            try {
                webClient.getPage("http://localhost:8080/protected");
                fail("Exception is expected because an invalid scope is provided");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
                assertTrue(ex.getResponse().getContentAsString()
                        .contains(
                                "Authorization code flow has failed, error code: invalid_scope, error description: Invalid scopes: openid invalid-scope"),
                        "The reason behind 401 must be returned in devmode");
            }
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
