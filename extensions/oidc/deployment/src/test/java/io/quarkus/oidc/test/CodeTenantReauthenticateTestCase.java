package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeTenantReauthenticateTestCase {
    private static Class<?>[] testClasses = {
            TenantReauthentication.class,
            CustomTenantResolver.class,
            CustomTenantConfigResolver.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-tenant-reauthenticate.properties", "application.properties"));

    @Test
    public void testDefaultTenant() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, null, "/protected", "alice");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testTenantResolver() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-resolver", "/protected/tenant/tenant-resolver", "tenant-resolver:alice");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testTenantConfigResolver() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-config-resolver", "/protected/tenant/tenant-config-resolver",
                    "tenant-config-resolver:alice");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromTenantResolverToDefaultTenant() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-resolver", "/protected/tenant/tenant-resolver", "tenant-resolver:alice");
            expectReauthentication(webClient, "/protected", "tenant-resolver");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromDefaultTenantToTenantResover() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, null, "/protected", "alice");
            expectReauthentication(webClient, "/protected/tenant/tenant-resolver", null);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromTenantConfigResolverToDefaultTenant() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-config-resolver", "/protected/tenant/tenant-config-resolver",
                    "tenant-config-resolver:alice");
            expectReauthentication(webClient, "/protected", "tenant-config-resolver");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromDefaultTenantToTenantConfigResolver() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, null, "/protected", "alice");
            expectReauthentication(webClient, "/protected/tenant/tenant-config-resolver", null);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromTenantResolverToTenantConfigResolver() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-resolver", "/protected/tenant/tenant-resolver", "tenant-resolver:alice");
            expectReauthentication(webClient, "/protected/tenant/tenant-config-resolver", "tenant-resolver");

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testSwitchFromTenantConfigResolverToTenantResolver() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            callTenant(webClient, "tenant-config-resolver", "/protected/tenant/tenant-config-resolver",
                    "tenant-config-resolver:alice");
            expectReauthentication(webClient, "/protected/tenant/tenant-resolver", "tenant-config-resolver");

            webClient.getCookieManager().clearCookies();
        }
    }

    private static void callTenant(WebClient webClient, String tenant, String relativePath, String expectedResponse)
            throws Exception {
        HtmlPage page = webClient.getPage("http://localhost:8081" + relativePath);

        assertEquals("Sign in to quarkus", page.getTitleText());

        HtmlForm loginForm = page.getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute("alice");
        loginForm.getInputByName("password").setValueAttribute("alice");

        page = loginForm.getButtonByName("login").click();

        assertEquals(expectedResponse, page.getBody().asNormalizedText());
        assertNotNull(getSessionCookie(webClient, tenant));
    }

    private static void expectReauthentication(WebClient webClient, String relativePath,
            String oldTenant) throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        TextPage textPage = webClient.getPage("http://localhost:8081" + relativePath);
        assertEquals(302, textPage.getWebResponse().getStatusCode());
        assertNull(getSessionCookie(webClient, oldTenant));
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static Cookie getSessionCookie(WebClient webClient, String tenantId) {
        String sessionCookie = "q_session" + (tenantId == null ? "" : "_" + tenantId);

        return webClient.getCookieManager().getCookie(sessionCookie);
    }
}
