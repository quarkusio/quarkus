package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowVerifyInjectedAccessTokenDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResourceWithJwtAccessToken.class)
                    .addAsResource("application-verify-injected-access-token-disabled.properties", "application.properties"))
            .setLogRecordPredicate(r -> true)
            .assertLogRecords(r -> assertLogRecord(r));

    @Test
    public void testVerifyAccessTokenDisabled() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {

            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice:false", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();
        }
    }

    private static void assertLogRecord(List<LogRecord> records) {
        List<LogRecord> authorizationRecords = records.stream()
                .filter(r -> (r.getMessage().contains("\"access_token\":")
                        || r.getMessage().contains("\"refresh_token\":")
                        || r.getMessage().contains("\"id_token\":")))
                .collect(Collectors.toList());
        assertFalse(authorizationRecords.isEmpty());

        List<LogRecord> accessTokenRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("\"access_token\":")).collect(Collectors.toList());
        assertFalse(accessTokenRecords.isEmpty());
        assertTrue(accessTokenRecords.stream().allMatch(r -> r.getMessage().contains("\"access_token\":\"...\"")));

        List<LogRecord> refreshTokenRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("\"refresh_token\":")).collect(Collectors.toList());
        assertFalse(refreshTokenRecords.isEmpty());
        assertTrue(refreshTokenRecords.stream().allMatch(r -> r.getMessage().contains("\"refresh_token\":\"...\"")));

        List<LogRecord> idTokenRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("\"id_token\":")).collect(Collectors.toList());
        assertFalse(idTokenRecords.isEmpty());
        assertTrue(idTokenRecords.stream().allMatch(r -> r.getMessage().contains("\"id_token\":\"...\"")));
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
