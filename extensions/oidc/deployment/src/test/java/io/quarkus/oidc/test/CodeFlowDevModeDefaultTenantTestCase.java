package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
