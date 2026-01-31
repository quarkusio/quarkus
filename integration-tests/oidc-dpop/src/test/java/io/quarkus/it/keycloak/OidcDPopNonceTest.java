package io.quarkus.it.keycloak;

import static io.netty.handler.codec.http.HttpHeaders.Values.NO_STORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.inject.Inject;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.http.HttpHeaders;

@TestProfile(OidcDPopNonceTest.UseDPoPNonceProvider.class)
@QuarkusTest
public class OidcDPopNonceTest {

    @Inject
    DummyDPoPNonceProvider dummyDPoPNonceProvider;

    @Test
    public void testDPopProofWithCorrectNonce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            dummyDPoPNonceProvider.setNonce("correct-nonce-scenario");
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/single-page-app/login-jwt-with-nonce/correct-nonce-scenario");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("Hello, alice; JWK thumbprint in JWT: true, JWK thumbprint in introspection: false",
                    textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWithWrongNonce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            String expectedNonce = "another-correct-nonce-scenario";
            dummyDPoPNonceProvider.setNonce(expectedNonce);
            HtmlPage page = webClient
                    .getPage("http://localhost:8081/single-page-app/login-jwt-with-nonce/wrong-nonce-scenario");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource", textPage.getContent());

            // assert expected headers
            String wwwAuthenticate = textPage.getWebResponse()
                    .getResponseHeaderValue(HttpHeaderNames.WWW_AUTHENTICATE.toString());
            assertNotNull(wwwAuthenticate);
            assertTrue(wwwAuthenticate.contains("DPoP error=\"invalid_dpop_proof\""),
                    () -> "Expected 'DPoP error=\"invalid_dpop_proof\"', but got: " + wwwAuthenticate);
            String dpopNonce = textPage.getWebResponse().getResponseHeaderValue(OidcConstants.DPOP_NONCE);
            assertEquals(expectedNonce, dpopNonce);
            String cacheControl = textPage.getWebResponse().getResponseHeaderValue(HttpHeaders.CACHE_CONTROL.toString());
            assertEquals(NO_STORE, cacheControl);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testDPopProofWithMissingNonce() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            String expectedNonce = "some-correct-nonce-scenario";
            dummyDPoPNonceProvider.setNonce(expectedNonce);
            HtmlPage page = webClient.getPage("http://localhost:8081/single-page-app/login-jwt");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            TextPage textPage = loginForm.getButtonByName("login").click();

            assertEquals("401 status from ProtectedResource", textPage.getContent());

            // assert expected headers
            String wwwAuthenticate = textPage.getWebResponse()
                    .getResponseHeaderValue(HttpHeaderNames.WWW_AUTHENTICATE.toString());
            assertNotNull(wwwAuthenticate);
            assertTrue(wwwAuthenticate.contains("DPoP error=\"use_dpop_nonce\""),
                    () -> "Expected 'DPoP error=\"use_dpop_nonce\"', but got: " + wwwAuthenticate);
            String dpopNonce = textPage.getWebResponse().getResponseHeaderValue(OidcConstants.DPOP_NONCE);
            assertEquals(expectedNonce, dpopNonce);
            String cacheControl = textPage.getWebResponse().getResponseHeaderValue(HttpHeaders.CACHE_CONTROL.toString());
            assertEquals(NO_STORE, cacheControl);

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    public static final class UseDPoPNonceProvider implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("use-dpop-nonce-provider", "true");
        }
    }
}
