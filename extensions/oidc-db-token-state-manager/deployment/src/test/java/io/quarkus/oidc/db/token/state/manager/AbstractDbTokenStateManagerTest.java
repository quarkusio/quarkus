package io.quarkus.oidc.db.token.state.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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

            textPage = loginForm.getInputByName("login").click();

            assertEquals("alice", textPage.getContent());

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
