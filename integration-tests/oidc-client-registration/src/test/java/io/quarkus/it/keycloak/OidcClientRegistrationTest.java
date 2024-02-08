package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OidcClientRegistrationTest {

    @Test
    public void testDefaultRegisteredClientOnStartup() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client:Default Client Updated:alice", textPage.getContent());
        }
    }

    @Test
    public void testTenantRegisteredClientOnStartup() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/tenant");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client-tenant:Tenant Client:alice", textPage.getContent());
        }
    }

    @Test
    public void testRegisteredClientDynamically() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/dynamic");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client-dynamically:Dynamic Client:alice", textPage.getContent());
        }
    }

    @Test
    public void testRegisteredClientDynamicTenant() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/dynamic-tenant");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client-dynamic-tenant:Dynamic Tenant Client:alice", textPage.getContent());
        }
    }

    @Test
    public void testRegisteredClientMulti1() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/multi1");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client-multi1:Multi1 Client:alice", textPage.getContent());
        }
    }

    @Test
    public void testRegisteredClientMulti2() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected/multi2");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("login").click();

            assertEquals("registered-client-multi2:Multi2 Client:alice", textPage.getContent());
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

}
