package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ThrowingRunnable;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
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
        checkLog();
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

    private void checkLog() {
        final Path logDirectory = Paths.get(".", "target");
        given().await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        boolean fileExists = Files.exists(accessLogFilePath);
                        if (!fileExists) {
                            accessLogFilePath = logDirectory.resolve("target/quarkus.log");
                            fileExists = Files.exists(accessLogFilePath);
                        }
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
                                "quarkus log file " + accessLogFilePath + " is missing");

                        boolean clientRegistrationRequest = false;
                        boolean clientRegistered = false;
                        boolean registeredClientUpdated = false;

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("'Default Client' registration request")) {
                                    clientRegistrationRequest = true;
                                } else if (line.contains("'Default Client' has been registered")) {
                                    clientRegistered = true;
                                } else if (line.contains(
                                        "Registered 'Default Client' has had its name updated to 'Default Client Updated'")) {
                                    registeredClientUpdated = true;
                                }
                                if (clientRegistrationRequest && clientRegistered && registeredClientUpdated) {
                                    break;
                                }

                            }
                        }
                        assertTrue(clientRegistrationRequest,
                                "Log file must contain a default client registration request confirmation");
                        assertTrue(clientRegistered,
                                "Log file must contain a default client registration confirmation");
                        assertTrue(registeredClientUpdated,
                                "Log file must contain a a default client's name update confirmation");
                    }
                });
    }
}
