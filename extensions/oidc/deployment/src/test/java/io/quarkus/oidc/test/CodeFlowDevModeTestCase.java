package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowDevModeTestCase {

    private static Class<?>[] testClasses = {
            ProtectedResource.class,
            UnprotectedResource.class,
            CustomTenantConfigResolver.class,
            CustomTokenStateManager.class,
            OidcConfigSource.class,
            SecretProvider.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-dev-mode.properties", "application.properties")
                    .addAsServiceProvider(ConfigSource.class, OidcConfigSource.class));

    @Test
    public void testAccessAndRefreshTokenInjectionDevMode() throws IOException, InterruptedException {
        // Default tenant is disabled, check that having TenantConfigResolver is enough
        useTenantConfigResolver();

        try (final WebClient webClient = createWebClient()) {

            // Default tenant is disabled and client secret is wrong
            HtmlPage page = webClient.getPage("http://localhost:8080/unprotected");
            assertEquals("unprotected", page.getBody().asText());

            try {
                webClient.getPage("http://localhost:8080/protected");
                fail("Exception is expected because the tenant is disabled and invalid client secret is used");
            } catch (FailingHttpStatusCodeException ex) {
                // Reported by Quarkus
                assertEquals(401, ex.getStatusCode());
            }

            // Enable the default tenant
            test.modifyResourceFile("application.properties", s -> s.replace("tenant-enabled=false", "tenant-enabled=true"));
            // Default tenant is enabled, client secret is wrong
            try {
                page = webClient.getPage("http://localhost:8080/protected");

                assertEquals("Sign in to quarkus", page.getTitleText());

                HtmlForm loginForm = page.getForms().get(0);

                loginForm.getInputByName("username").setValueAttribute("alice");
                loginForm.getInputByName("password").setValueAttribute("alice");

                page = loginForm.getInputByName("login").click();
                fail("Exception is expected because an invalid client secret is used");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();

            // Now set the correct client-id
            test.modifyResourceFile("application.properties", s -> s.replace("secret-from-vault-typo", "secret-from-vault"));

            page = webClient.getPage("http://localhost:8080/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asText());

            assertEquals("custom", page.getWebClient().getCookieManager().getCookie("q_session").getValue().split("\\|")[3]);

            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8080/protected/logout").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(webClient.getCookieManager().getCookie("q_session"));

            webClient.getCookieManager().clearCookies();
        }
    }

    private void useTenantConfigResolver() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/protected/tenant/tenant-config-resolver");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("tenant-config-resolver:alice", page.getBody().asText());
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
