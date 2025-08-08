package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class CodeFlowManagementInterfaceDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CodeFlowManagementRoute.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.management.enabled=true
                                    quarkus.management.auth.enabled=true
                                    quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                                    quarkus.oidc.client-id=quarkus-web-app
                                    quarkus.oidc.credentials.secret=secret
                                    quarkus.oidc.application-type=web-app
                                    quarkus.management.auth.permission.code-flow.paths=/code-flow
                                    quarkus.management.auth.permission.code-flow.policy=authenticated
                                    quarkus.management.auth.permission.code-flow.auth-mechanism=code
                                    quarkus.log.category."org.htmlunit".level=ERROR
                                    quarkus.log.file.enable=true
                                    """),
                            "application.properties"));

    @Test
    public void testAuthenticatedHttpPermission() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:9000/code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("alice", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    @Singleton
    public static class CodeFlowManagementRoute {
        void setupManagementRoutes(@Observes ManagementInterface managementInterface, IdentityProviderManager ipm) {
            managementInterface.router().get("/code-flow").handler(rc -> QuarkusHttpUser
                    .getSecurityIdentity(rc, ipm)
                    .map(i -> i.getPrincipal().getName())
                    .subscribe().with(rc::end, err -> rc.fail(500, err)));
        }
    }
}
