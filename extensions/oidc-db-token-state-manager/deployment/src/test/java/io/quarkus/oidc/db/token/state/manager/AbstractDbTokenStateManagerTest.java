package io.quarkus.oidc.db.token.state.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public abstract class AbstractDbTokenStateManagerTest {

    protected static QuarkusUnitTest createQuarkusUnitTest(String reactiveSqlClientExtension) {
        return createQuarkusUnitTest(reactiveSqlClientExtension, null);
    }

    protected static QuarkusUnitTest createQuarkusUnitTest(String reactiveSqlClientExtension,
            Consumer<JavaArchive> customizer) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> {
                    jar
                            .addClasses(ProtectedResource.class, UnprotectedResource.class, PublicResource.class)
                            .addAsResource("application.properties");
                    if (customizer != null) {
                        customizer.accept(jar);
                    }
                })
                .setForcedDependencies(
                        List.of(Dependency.of("io.quarkus", reactiveSqlClientExtension, Version.getVersion())));
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
                .get("public/db-state-manager-table-content")
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
