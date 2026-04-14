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
public class CodeFlowVerifyAccessTokenDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResourceWithoutJwtAccessToken.class)
                    .addAsResource("application-verify-access-token-disabled.properties", "application.properties"))
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
                .filter(r -> (r.getMessage().contains("client_secret=")
                        || r.getMessage().contains("code=")
                        || r.getMessage().contains("code_verifier=")))
                .collect(Collectors.toList());
        assertFalse(authorizationRecords.isEmpty());

        List<LogRecord> clientSecretRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("client_secret=")).collect(Collectors.toList());
        assertFalse(clientSecretRecords.isEmpty());

        assertTrue(clientSecretRecords.stream().allMatch(r -> r.getMessage().contains("client_secret=...")));

        List<LogRecord> codeRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("code=")).collect(Collectors.toList());
        assertFalse(codeRecords.isEmpty());

        assertTrue(codeRecords.stream().allMatch(r -> r.getMessage().contains("code=...")));

        List<LogRecord> codeVerifierRecords = authorizationRecords.stream()
                .filter(r -> r.getMessage().contains("code_verifier=")).collect(Collectors.toList());
        assertFalse(codeVerifierRecords.isEmpty());

        assertTrue(codeVerifierRecords.stream().allMatch(r -> r.getMessage().contains("code_verifier=...")));
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
