package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import static io.quarkus.oidc.token.propagation.reactive.deployment.test.RolesSecurityIdentityAugmentor.SUPPORTED_USER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class OidcTokenPropagationWithSecurityIdentityAugmentorTest {

    private static Class<?>[] testClasses = {
            FrontendResource.class,
            ProtectedResource.class,
            AccessTokenPropagationService.class,
            RolesResource.class,
            RolesService.class,
            RolesSecurityIdentityAugmentor.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application.properties")
                    .addAsResource(
                            new StringAsset("quarkus.rest-client-oidc-token-propagation.enabled-during-authentication=true\n" +
                                    "quarkus.rest-client.\"roles\".uri=http://localhost:8081/roles\n"),
                            "META-INF/microprofile-config.properties"));

    @Test
    public void testGetUserNameWithTokenPropagation() {
        // request only succeeds if SecurityIdentityAugmentor managed to acquire 'tester' role for user 'alice'
        // and that is only possible if access token is propagated during augmentation
        RestAssured.given().auth().oauth2(getBearerAccessToken())
                .when().get("/frontend/token-propagation-with-augmentor")
                .then()
                .statusCode(200)
                .body(equalTo("Token issued to alice has been exchanged, new user name: bob"));
    }

    public String getBearerAccessToken() {
        return OidcWiremockTestResource.getAccessToken(SUPPORTED_USER, Set.of("admin"));
    }

    @Test
    public void testGetUserNameWithTokenPropagationWithCodeFlow() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/frontend/token-propagation-with-augmentor");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            TextPage textPage = form.getInputByValue("login").click();

            assertEquals("Token issued to alice has been exchanged, new user name: bob", textPage.getContent());
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
