package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.htmlunit.WebClient;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowApplicationLoginPageProactiveAuthTest extends AbstractCodeFlowApplicationLoginPageTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> application(jar, true));

    @Test
    public void testStaleCookiesOnLoginPage() throws Exception {
        try (WebClient webClient = createWebClient()) {
            addCookie(webClient, "q_session", "invalid");
            WebResponse response = get(webClient, "http://localhost:8081/login");
            assertEquals(401, response.getStatusCode());
            assertEquals("OIDC", response.getResponseHeaderValue("WWW-Authenticate"));
            assertNull(response.getResponseHeaderValue("location"));
            assertFalse(hasCookie(webClient, "q_session"));
            assertEquals(200, get(webClient, "http://localhost:8081/login").getStatusCode());

            addCookie(webClient, "q_auth_tenant-strict", "invalid");
            response = get(webClient, "http://localhost:8081/tenant-strict/login");
            assertEquals(401, response.getStatusCode());
            assertFalse(hasCookie(webClient, "q_auth_tenant-strict"));
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testStaleStateCookieFailsOutsideLoginPage() throws Exception {
        try (WebClient webClient = createWebClient()) {
            addCookie(webClient, "q_auth_tenant-strict", "invalid");
            WebResponse response = get(webClient, "http://localhost:8081/tenant-strict/public");
            assertEquals(401, response.getStatusCode());
            webClient.getCookieManager().clearCookies();
        }
    }
}
