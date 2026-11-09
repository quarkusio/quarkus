package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowApplicationLoginPageLazyAuthTest extends AbstractCodeFlowApplicationLoginPageTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> application(jar, false));

    @Test
    public void testStaleSessionCookieOnLoginPage() throws Exception {
        try (WebClient webClient = createWebClient()) {
            addCookie(webClient, "q_session", "invalid");
            assertEquals(200, get(webClient, "http://localhost:8081/login").getStatusCode());
            webClient.getCookieManager().clearCookies();
        }
    }
}
