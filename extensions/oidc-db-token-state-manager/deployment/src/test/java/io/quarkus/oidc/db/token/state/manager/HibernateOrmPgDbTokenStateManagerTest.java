package io.quarkus.oidc.db.token.state.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class HibernateOrmPgDbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedResource.class, UnprotectedResource.class, PublicResource.class,
                            GreetingResource.class, GreetingEntity.class, OidcDbTokenStateManagerEntity.class,
                            OidcDbTokenStateManagerResource.class)
                    .addAsResource("hibernate-orm-application.properties", "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-reactive-pg-client", Version.getVersion()),
                            Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion())));

    @Test
    public void testCodeFlowOnTableNotCreatedByExtension() throws IOException {
        // also tests that this extension works with Hibernate ORM (creates / updates entity)
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page;
            page = webClient.getPage(url.toString() + "greeting/new");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();
            assertEquals(200, page.getWebResponse().getStatusCode());

            WebResponse webResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create(url.toString() + "greeting").toURL()));
            assertEquals(200, webResponse.getStatusCode());
            assertTrue(webResponse.getContentAsString().contains("Good day"));

            webClient.getOptions().setRedirectEnabled(false);
            webResponse = webClient
                    .loadWebResponse(new WebRequest(URI.create(url.toString() + "protected/logout").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            assertNull(webClient.getCookieManager().getCookie("q_session"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testExpiredTokenDeletion() {
        assertTokenStateCount(0);

        // create 3 tokens
        RestAssured
                .given()
                .body(3)
                .post("/token-state-manager-generator")
                .then()
                .statusCode(204);
        assertTokenStateCount(3);

        // make sure expired tokens are deleted
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertTokenStateCount(0));
    }
}
