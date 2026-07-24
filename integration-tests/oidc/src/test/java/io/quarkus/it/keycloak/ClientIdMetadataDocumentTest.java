package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(KeycloakXTestResourceLifecycleManager.class)
public class ClientIdMetadataDocumentTest {

    @Test
    public void testClientIdMetadataDocument() throws Exception {
        try (WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("http://localhost:8081/tenant-cimd").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");

            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage(keycloakUrl);
            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            // Keycloak CIMD requires consent
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage consentPage = loginForm.getButtonByName("login").click();
            HtmlForm consentForm = consentPage.getForms().get(0);
            webClient.getOptions().setRedirectEnabled(false);
            webResponse = consentForm.getButtonByName("accept").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // Replace host.testcontainers.internal with localhost for HtmlUnit
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(endpointLocation);
            endpointLocation = endpointLocation.replace("host.testcontainers.internal", "localhost");

            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(endpointLocation).toURL()));
            String finalLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(finalLocation);
            finalLocation = finalLocation.replace("host.testcontainers.internal", "localhost");

            webClient.getOptions().setRedirectEnabled(true);
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(finalLocation).toURL()));
            assertEquals("hello", webResponse.getContentAsString());
            webClient.getCookieManager().clearCookies();

            // check metadata
            WebResponse metadataResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("https://localhost:8444/client-id-metadata/tenant-cimd").toURL()));
            assertEquals(200, metadataResponse.getStatusCode());

            JsonObject metadata = new JsonObject(metadataResponse.getContentAsString());
            assertEquals("https://host.testcontainers.internal:8444/client-id-metadata/tenant-cimd",
                    metadata.getString("client_id"));
            assertEquals("Tenant CIMD", metadata.getString("client_name"));
            assertEquals("none", metadata.getString("token_endpoint_auth_method"));
            JsonArray redirectUris = metadata.getJsonArray("redirect_uris");
            assertEquals(1, redirectUris.size());
            assertEquals("https://host.testcontainers.internal:8444/tenant-cimd",
                    redirectUris.getString(0));
        }
    }

    @Test
    public void testClientIdMetadataDocumentWithPrivateKeyJwt() throws Exception {
        try (WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);

            WebResponse webResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("http://localhost:8081/tenant-cimd-jwt").toURL()));
            String keycloakUrl = webResponse.getResponseHeaderValue("location");

            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage(keycloakUrl);
            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            // Keycloak CIMD requires consent
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage consentPage = loginForm.getButtonByName("login").click();
            HtmlForm consentForm = consentPage.getForms().get(0);
            webClient.getOptions().setRedirectEnabled(false);
            webResponse = consentForm.getButtonByName("accept").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(endpointLocation);
            endpointLocation = endpointLocation.replace("host.testcontainers.internal", "localhost");

            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(endpointLocation).toURL()));
            String finalLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(finalLocation);
            finalLocation = finalLocation.replace("host.testcontainers.internal", "localhost");

            webClient.getOptions().setRedirectEnabled(true);
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(finalLocation).toURL()));
            assertEquals("hello", webResponse.getContentAsString());
            webClient.getCookieManager().clearCookies();

            // check metadata
            WebResponse metadataResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("https://localhost:8444/client-id-metadata/tenant-cimd-jwt").toURL()));
            assertEquals(200, metadataResponse.getStatusCode());

            JsonObject metadata = new JsonObject(metadataResponse.getContentAsString());
            assertEquals("https://host.testcontainers.internal:8444/client-id-metadata/tenant-cimd-jwt",
                    metadata.getString("client_id"));
            assertEquals("Tenant CIMD JWT", metadata.getString("client_name"));
            assertEquals("private_key_jwt", metadata.getString("token_endpoint_auth_method"));

            JsonArray redirectUris = metadata.getJsonArray("redirect_uris");
            assertEquals(1, redirectUris.size());
            assertEquals("https://host.testcontainers.internal:8444/tenant-cimd-jwt",
                    redirectUris.getString(0));

            JsonObject jwks = metadata.getJsonObject("jwks");
            assertNotNull(jwks);
            JsonArray keys = jwks.getJsonArray("keys");
            assertEquals(1, keys.size());
            JsonObject jwk = keys.getJsonObject(0);
            assertEquals("RSA", jwk.getString("kty"));
            assertNotNull(jwk.getString("n"));
            assertNotNull(jwk.getString("e"));
        }
    }

    @Test
    public void testClientIdMetadataDocumentDynamic() throws Exception {
        try (WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            WebResponse webResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("http://localhost:8081/tenant-cimd-dynamic").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
            String keycloakUrl = webResponse.getResponseHeaderValue("location");

            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage(keycloakUrl);
            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            // Keycloak CIMD requires consent
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage consentPage = loginForm.getButtonByName("login").click();
            HtmlForm consentForm = consentPage.getForms().get(0);
            webClient.getOptions().setRedirectEnabled(false);
            webResponse = consentForm.getButtonByName("accept").click().getWebResponse();
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

            // Replace host.testcontainers.internal with localhost for HtmlUnit
            String endpointLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(endpointLocation);
            endpointLocation = endpointLocation.replace("host.testcontainers.internal", "localhost");

            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(endpointLocation).toURL()));
            String finalLocation = webResponse.getResponseHeaderValue("location");
            assertNotNull(finalLocation);
            finalLocation = finalLocation.replace("host.testcontainers.internal", "localhost");

            webClient.getOptions().setRedirectEnabled(true);
            webResponse = webClient.loadWebResponse(new WebRequest(URI.create(finalLocation).toURL()));
            assertEquals("hello", webResponse.getContentAsString());
            webClient.getCookieManager().clearCookies();

            // check metadata
            WebResponse metadataResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("https://localhost:8444/client-id-metadata/tenant-cimd-dynamic").toURL()));
            assertEquals(200, metadataResponse.getStatusCode());

            JsonObject metadata = new JsonObject(metadataResponse.getContentAsString());
            assertEquals("https://host.testcontainers.internal:8444/client-id-metadata/tenant-cimd-dynamic",
                    metadata.getString("client_id"));
            assertEquals("Tenant CIMD Dynamic", metadata.getString("client_name"));
            assertEquals("none", metadata.getString("token_endpoint_auth_method"));
            JsonArray redirectUris = metadata.getJsonArray("redirect_uris");
            assertEquals(1, redirectUris.size());
            assertEquals("https://host.testcontainers.internal:8444/tenant-cimd-dynamic",
                    redirectUris.getString(0));
        }
    }

    private WebClient createWebClient() throws Exception {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificateKeyStore(
                java.nio.file.Paths.get("target/certificates/oidc-client-keystore.p12").toFile().toURI().toURL(),
                "password", "PKCS12");
        return webClient;
    }
}
