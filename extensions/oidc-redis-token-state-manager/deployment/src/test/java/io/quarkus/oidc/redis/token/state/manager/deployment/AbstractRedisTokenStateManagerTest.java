package io.quarkus.oidc.redis.token.state.manager.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public abstract class AbstractRedisTokenStateManagerTest {

    protected static QuarkusUnitTest createQuarkusUnitTest(String... extraProps) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(ProtectedResource.class, UnprotectedResource.class, PublicResource.class)
                        .addAsResource(new StringAsset("""
                                quarkus.oidc.client-id=quarkus-web-app
                                quarkus.oidc.application-type=web-app
                                quarkus.oidc.logout.path=/protected/logout
                                quarkus.log.category."org.htmlunit.javascript.host.css.CSSStyleSheet".level=FATAL
                                quarkus.log.category."org.htmlunit.css".level=FATAL
                                """ + String.join(System.lineSeparator(), extraProps)), "application.properties"));
    }

    @TestHTTPResource
    URL url;

    @Test
    public void testCodeFlow() throws IOException {
        try (final WebClient webClient = createWebClient()) {

            TextPage textPage = webClient.getPage(url.toString() + "unprotected");
            assertEquals("unprotected", textPage.getContent());

            HtmlPage page;
            page = webClient.getPage(url.toString() + "protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            textPage = loginForm.getButtonByName("login").click();

            assertEquals(
                    "alice, access token: true, access_token_expires_in: true, access_token_scope: true, refresh_token: true",
                    textPage.getContent());

            assertTokenStateCount(1);

            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create(url.toString() + "protected/logout").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(webClient.getCookieManager().getCookie("q_session"));

            webClient.getCookieManager().clearCookies();

            assertTokenStateCount(0);
        }
    }

    protected static void assertTokenStateCount(Integer tokenStateCount) {
        RestAssured
                .given()
                .get("public/oidc-token-states-count")
                .then()
                .statusCode(200)
                .body(Matchers.is(tokenStateCount.toString()));
    }

    protected static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

}
