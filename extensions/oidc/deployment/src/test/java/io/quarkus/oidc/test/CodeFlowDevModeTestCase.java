package io.quarkus.oidc.test;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.core.ThrowingRunnable;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    @Order(1)
    @Test
    public void testAccessAndRefreshTokenInjectionDevMode() throws IOException, InterruptedException {
        // Default tenant is disabled, check that having TenantConfigResolver is enough
        useTenantConfigResolver();

        try (final WebClient webClient = createWebClient()) {

            // Default tenant is disabled and client secret is wrong
            HtmlPage page = webClient.getPage("http://localhost:8080/unprotected");
            assertEquals("unprotected", page.getBody().asNormalizedText());

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

                page = loginForm.getButtonByName("login").click();
                fail("Exception is expected because an invalid client secret is used");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();

            // Now set the correct client secret
            test.modifyResourceFile("application.properties", s -> s.replace("secret-from-vault-typo", "secret-from-vault"));

            page = webClient.getPage("http://localhost:8080/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            Cookie sessionCookie = getSessionCookie(webClient, null);
            assertNotNull(sessionCookie);
            String sessionCookieValue = sessionCookie.getValue();
            assertEquals("custom", sessionCookieValue.split("\\|")[1]);

            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create("http://localhost:8080/protected/logout").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(webClient.getCookieManager().getCookie("q_session"));

            webClient.getCookieManager().clearCookies();
        }
        checkPkceSecretGenerated();

    }

    @Order(2)
    @Test
    public void testAccessTokenVerified() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            test.modifyResourceFile("application.properties", s -> s.replace("tenant-enabled=false", "tenant-enabled=true")
                    .replace("secret-from-vault-typo", "secret-from-vault"));
            HtmlPage page = webClient.getPage("http://localhost:8080/protected/access-token-name");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());
        }
    }

    private void useTenantConfigResolver() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/protected/tenant/tenant-config-resolver");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("tenant-config-resolver:alice", page.getBody().asNormalizedText());
            webClient.getCookieManager().clearCookies();
            try {
                webClient.getPage("http://localhost:8080/protected/tenant/null-tenant");
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
            try {
                webClient.getPage("http://localhost:8080/protected/tenant/unknown-tenant");
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
            }
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    protected static void checkPkceSecretGenerated() {
        AtomicBoolean checkPassed = new AtomicBoolean();
        given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        final Path logDirectory = Paths.get(".", "target");
                        Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        boolean fileExists = Files.exists(accessLogFilePath);
                        if (!fileExists) {
                            accessLogFilePath = logDirectory.resolve("target/quarkus.log");
                            fileExists = Files.exists(accessLogFilePath);
                        }
                        assertTrue(fileExists, "quarkus log is missing");

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains(
                                        "Secret key for encrypting state cookie is missing, auto-generating it")) {
                                    checkPassed.set(true);
                                }
                            }
                        }
                    }
                });
        assertTrue(checkPassed.get(), "Can not confirm Secret key for encrypting state cookie has been generated");
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        String sessionCookieName = "q_session" + (tenantId == null ? "" : "_" + tenantId);

        return webClient.getCookieManager().getCookie(sessionCookieName);
    }
}
