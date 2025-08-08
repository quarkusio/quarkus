package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.IdToken;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class ImplicitBasicAuthAndCodeFlowAuthCombinationTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicCodeFlowResource.class)
                    .addAsResource(
                            new StringAsset("""
                                    # Disable Dev Services, we use a test resource manager
                                    quarkus.keycloak.devservices.enabled=false

                                    quarkus.security.users.embedded.enabled=true
                                    quarkus.security.users.embedded.plain-text=true
                                    quarkus.security.users.embedded.users.alice=alice
                                    quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                                    quarkus.oidc.client-id=quarkus-web-app
                                    quarkus.oidc.credentials.secret=secret
                                    quarkus.oidc.application-type=web-app
                                    quarkus.http.auth.permission.code-flow.paths=/basic-code-flow/code-flow
                                    quarkus.http.auth.permission.code-flow.policy=authenticated
                                    quarkus.http.auth.permission.code-flow.auth-mechanism=code
                                    quarkus.http.auth.permission.basic.paths=/basic-code-flow/basic
                                    quarkus.http.auth.permission.basic.policy=authenticated
                                    quarkus.http.auth.permission.basic.auth-mechanism=basic
                                    """),
                            "application.properties"));

    @Test
    public void testBasicEnabledAsSelectedWithHttpPerm() throws IOException, InterruptedException {
        // endpoint is annotated with 'BasicAuthentication', so basic auth must be enabled
        RestAssured.given().auth().basic("alice", "alice").get("/basic-code-flow/basic")
                .then().statusCode(204);
        RestAssured.given().auth().basic("alice", "alice").redirects().follow(false)
                .get("/basic-code-flow/code-flow").then().statusCode(302);

        try (final WebClient webClient = createWebClient()) {

            try {
                webClient.getPage("http://localhost:8080/basic-code-flow/basic");
                fail("Exception is expected because by the basic auth is required");
            } catch (FailingHttpStatusCodeException ex) {
                // Reported by Quarkus
                assertEquals(401, ex.getStatusCode());
            }
            HtmlPage page = webClient.getPage("http://localhost:8080/basic-code-flow/code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static String getAccessToken() {
        return KeycloakTestResourceLifecycleManager.getAccessToken("alice");
    }

    @Path("basic-code-flow")
    public static class BasicCodeFlowResource {

        @Inject
        @IdToken
        JsonWebToken idToken;

        @GET
        @Path("basic")
        public String basic() {
            return idToken.getName();
        }

        @GET
        @Path("code-flow")
        public String codeFlow() {
            return idToken.getName();
        }
    }

}
