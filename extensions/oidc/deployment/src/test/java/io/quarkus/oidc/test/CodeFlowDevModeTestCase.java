package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@QuarkusTestResource(KeycloakDevModeRealmResourceManager.class)
public class CodeFlowDevModeTestCase {

    private static Class<?>[] testClasses = {
            ProtectedResource.class,
            UnprotectedResource.class,
            CustomTenantConfigResolver.class,
            CustomTokenStateManager.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application-dev-mode.properties", "application.properties"));

    @Test
    public void testAccessAndRefreshTokenInjectionDevMode() throws IOException, InterruptedException {
        // Default tenant is disabled, check that having TenantConfigResolver is enough
        useTenantConfigResolver();

        try (final WebClient webClient = createWebClient()) {

            // Default tenant is disabled and client-id is wrong
            HtmlPage page = webClient.getPage("http://localhost:8080/unprotected");
            assertEquals("unprotected", page.getBody().asText());

            try {
                webClient.getPage("http://localhost:8080/protected");
                fail("Exception is expected because the tenant is disabled and invalid client_id is used");
            } catch (FailingHttpStatusCodeException ex) {
                // Reported by Quarkus
                assertEquals(401, ex.getStatusCode());
            }

            // Enable the default tenant
            test.modifyResourceFile("application.properties", s -> s.replace("tenant-enabled=false", "tenant-enabled=true"));
            // Default tenant is enabled, client-id is wrong
            try {
                webClient.getPage("http://localhost:8080/protected");
                fail("Exception is expected because the tenant is disabled and invalid client_id is used");
            } catch (FailingHttpStatusCodeException ex) {
                // Reported by Keycloak
                assertEquals(400, ex.getStatusCode());
                assertTrue(ex.getResponse().getContentAsString().contains("Client not found"));
            }

            // Now set the correct client-id
            test.modifyResourceFile("application.properties", s -> s.replace("client-dev", "client-dev-mode"));

            page = webClient.getPage("http://localhost:8080/protected");

            assertEquals("Log in to devmode", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice-dev-mode");
            loginForm.getInputByName("password").setValueAttribute("alice-dev-mode");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice-dev-mode", page.getBody().asText());

            assertEquals("custom", page.getWebClient().getCookieManager().getCookie("q_session").getValue().split("\\|")[3]);

            webClient.getCookieManager().clearCookies();
        }
    }

    private void useTenantConfigResolver() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/protected/tenant/tenant-config-resolver");

            assertEquals("Log in to devmode", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice-dev-mode");
            loginForm.getInputByName("password").setValueAttribute("alice-dev-mode");

            page = loginForm.getInputByName("login").click();

            assertEquals("tenant-config-resolver:alice-dev-mode", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
