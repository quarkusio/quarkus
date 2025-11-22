package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.smallrye.mutiny.Uni;

@QuarkusTestResource(value = KeycloakTestResourceLifecycleManager.class)
public class CodeFlowCredentialsProviderRefreshTest {

    private static final String DEFAULT_TENANT = "default";
    private static final String NAMED_TENANT = "named";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResource.class,
                            ClientSecretProviderProducer.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            # Disable Dev Services, we use a test resource manager
                                            quarkus.keycloak.devservices.enabled=false

                                            quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.client-id=quarkus-web-app
                                            quarkus.oidc.credentials.client-secret.provider.name=client-secret-provider
                                            quarkus.oidc.credentials.client-secret.provider.keyring-name=oidc-default-tenant
                                            quarkus.oidc.credentials.client-secret.provider.key=first-wrong-then-right
                                            quarkus.oidc.application-type=web-app
                                            quarkus.oidc.logout.path=/protected/logout
                                            quarkus.oidc.authentication.pkce-required=true

                                            quarkus.oidc.named.auth-server-url=${quarkus.oidc.auth-server-url}
                                            quarkus.oidc.named.client-id=${quarkus.oidc.client-id}
                                            quarkus.oidc.named.credentials.client-secret.provider.name=${quarkus.oidc.credentials.client-secret.provider.name}
                                            quarkus.oidc.named.credentials.client-secret.provider.keyring-name=oidc-named-tenant
                                            quarkus.oidc.named.credentials.client-secret.provider.key=${quarkus.oidc.credentials.client-secret.provider.key}
                                            quarkus.oidc.named.application-type=${quarkus.oidc.application-type}
                                            quarkus.oidc.named.logout.path=${quarkus.oidc.logout.path}
                                            quarkus.oidc.named.authentication.pkce-required=${quarkus.oidc.authentication.pkce-required}
                                            quarkus.oidc.named.credentials.client-secret.method=post

                                            quarkus.log.category."org.htmlunit.javascript.host.css.CSSStyleSheet".level=FATAL
                                            quarkus.log.category."org.htmlunit.css".level=FATAL
                                            quarkus.log.file.enabled=true
                                            """),
                            "application.properties"));

    @Test
    void testFailureHandlerForClientSecretBasicAuthScheme() throws IOException {
        testCredentialsRefreshedOn401(DEFAULT_TENANT);
    }

    @Test
    void testFailureHandlerForClientSecretPostAuth() throws IOException {
        testCredentialsRefreshedOn401(NAMED_TENANT);
    }

    private static void testCredentialsRefreshedOn401(String tenant) throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/tenant/" + tenant);

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals(tenant + ":alice", page.getBody().asNormalizedText());
        }
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    public static class ClientSecretProviderProducer {

        @ApplicationScoped
        @Named("client-secret-provider")
        @Produces
        CredentialsProvider createCredentialsProvider() {
            return new CredentialsProvider() {

                private final AtomicInteger defaultTenantCounter = new AtomicInteger();
                private final AtomicInteger namedTenantCounter = new AtomicInteger();

                @Override
                public Uni<Map<String, String>> getCredentialsAsync(String credentialsProviderName) {
                    final Map<String, String> result;

                    if ("oidc-default-tenant".equals(credentialsProviderName)) {
                        boolean setCorrect = defaultTenantCounter.incrementAndGet() == 2;
                        String secret = setCorrect ? "secret" : "wrong-secret";
                        result = Map.of("first-wrong-then-right", secret);
                    } else if ("oidc-named-tenant".equals(credentialsProviderName)) {
                        boolean setCorrect = namedTenantCounter.incrementAndGet() == 2;
                        String secret = setCorrect ? "secret" : "wrong-secret";
                        result = Map.of("first-wrong-then-right", secret);
                    } else {
                        result = Map.of();
                    }

                    return Uni.createFrom().item(result);
                }
            };
        }
    }
}
