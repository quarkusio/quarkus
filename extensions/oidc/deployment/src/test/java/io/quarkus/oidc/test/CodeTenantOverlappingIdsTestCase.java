package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.htmlunit.Page;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

/**
 * Verifies that the tenant id is correctly restored from the state and session cookies when it contains underscores
 * and overlaps with the id of a service tenant, for example, `tenant-a_1` and `tenant-a`.
 */
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeTenantOverlappingIdsTestCase {
    private static Class<?>[] testClasses = {
            TenantReauthentication.class,
            OverlappingIdsTenantResolver.class
    };

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-tenant-overlapping-ids.properties", "application.properties"));

    @Test
    public void testTenantWithUnderscoreInId() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            loginToTenant(webClient, "tenant-a_1");
            assertNull(getSessionCookie(webClient, "tenant-a"));

            // Tenant id is restored from the `q_session_tenant-a_1` session cookie
            callTenantWithExistingSession(webClient, "tenant-a_1");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testServiceTenantWithShorterId() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            // `tenant-a` is a service tenant, it has no cookies and requires a bearer token
            Page page = webClient.getPage("http://localhost:8081/protected/tenant/tenant-a");
            assertEquals(401, page.getWebResponse().getStatusCode());
            assertNull(getSessionCookie(webClient, "tenant-a"));
        }
    }

    private static void loginToTenant(WebClient webClient, String tenant) throws Exception {
        HtmlPage page = webClient.getPage("http://localhost:8081/protected/tenant/" + tenant);

        assertEquals("Sign in to quarkus", page.getTitleText());

        HtmlForm loginForm = page.getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute("alice");
        loginForm.getInputByName("password").setValueAttribute("alice");

        // The tenant id is restored from the `q_auth_<tenant>` state cookie when the code flow is completed
        page = loginForm.getButtonByName("login").click();

        assertEquals(tenant + ":alice", page.getBody().asNormalizedText());
        assertNotNull(getSessionCookie(webClient, tenant));
    }

    private static void callTenantWithExistingSession(WebClient webClient, String tenant) throws Exception {
        HtmlPage page = webClient.getPage("http://localhost:8081/protected/tenant/" + tenant);
        assertEquals(tenant + ":alice", page.getBody().asNormalizedText());
        assertNotNull(getSessionCookie(webClient, tenant));
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session_" + tenantId);
    }
}
