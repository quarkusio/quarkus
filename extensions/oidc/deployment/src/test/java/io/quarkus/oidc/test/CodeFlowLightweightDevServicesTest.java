package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.IdToken;
import io.quarkus.test.QuarkusUnitTest;

public class CodeFlowLightweightDevServicesTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.oidc.application-type=web-app
                            quarkus.oidc.devservices.lightweight.enabled=true
                            """), "application.properties"));

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String serverUrl;

    @Test
    public void testLoginAsCustomUser() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/admin-only");

            assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "custom-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            loginForm.getInputByName("name").setValueAttribute("Ronald");
            loginForm.getInputByName("roles").setValueAttribute("admin,user");

            page = loginForm.getButtonByName("login").click();
            assertTrue(page.getBody().asNormalizedText().contains("Ronald"));
            assertTrue(page.getBody().asNormalizedText().contains("admin"));
            assertTrue(page.getBody().asNormalizedText().contains("user"));

            assertNotNull(webClient.getCookieManager().getCookie("q_session"));

            testLogout(webClient);
        }
    }

    @Test
    public void testLoginAsAlice() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/admin-only");

            assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "predefined-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            page = loginForm.getButtonByName("predefined-alice").click();
            assertTrue(page.getBody().asNormalizedText().contains("alice"));
            assertTrue(page.getBody().asNormalizedText().contains("admin"));
            assertTrue(page.getBody().asNormalizedText().contains("user"));

            testLogout(webClient);
        }
    }

    @Test
    public void testLoginAsBob() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/secured/user-only");

            assertEquals("Login", page.getTitleText());

            HtmlForm loginForm = page.getForms().stream().filter(form -> "predefined-form".equals(form.getAttribute("class")))
                    .findFirst().get();

            page = loginForm.getButtonByName("predefined-bob").click();
            assertTrue(page.getBody().asNormalizedText().contains("bob"));
            assertTrue(page.getBody().asNormalizedText().contains("user"));
            assertFalse(page.getBody().asNormalizedText().contains("admin"));

            try {
                webClient.getPage("http://localhost:8081/secured/admin-only");
                fail("Exception is expected because Bob is not admin");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(403, ex.getStatusCode());
            }

            testLogout(webClient);
        }
    }

    private void testLogout(WebClient webClient) throws IOException {
        webClient.getOptions().setRedirectEnabled(false);
        WebResponse webResponse = webClient
                .loadWebResponse(new WebRequest(URI.create(serverUrl
                        + "/logout?post_logout_redirect_uri=north-pole&id_token_hint=SECRET")
                        .toURL()));
        assertEquals(302, webResponse.getStatusCode());
        assertEquals("north-pole", webResponse.getResponseHeaderValue("Location"));

        webClient.getCookieManager().clearCookies();
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    @Path("secured")
    public static class SecuredResource {

        @Inject
        @IdToken
        JsonWebToken idToken;

        @RolesAllowed("admin")
        @GET
        @Path("admin-only")
        public String getAdminOnly() {
            return idToken.getName() + " " + idToken.getGroups();
        }

        @RolesAllowed("user")
        @GET
        @Path("user-only")
        public String getUserOnly() {
            return idToken.getName() + " " + idToken.getGroups();
        }

    }

}
