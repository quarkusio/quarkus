package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.OidcDPopTest.loginAndClick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.inject.Inject;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(OidcDPopJtiTest.UseJtiDPoPNonceProvider.class)
@QuarkusTest
public class OidcDPopJtiTest {

    @Inject
    JtiTrackingDPoPNonceProvider dPoPNonceProvider;

    @Test
    void testReusedNonceWithFreshJti() throws Exception {
        dPoPNonceProvider.clear();
        dPoPNonceProvider.setNonce("reused-nonce");
        try {
            assertProofAccepted("reused-nonce", "fresh-jti-1");
            assertProofAccepted("reused-nonce", "fresh-jti-2");
        } finally {
            dPoPNonceProvider.clear();
        }
    }

    @Test
    void testReplayedJtiRejected() throws Exception {
        dPoPNonceProvider.clear();
        dPoPNonceProvider.setNonce("replay-nonce");
        try {
            assertProofAccepted("replay-nonce", "replayed-jti");
            assertProofReplayRejected("replay-nonce", "replayed-jti");
        } finally {
            dPoPNonceProvider.clear();
        }
    }

    @Test
    void testJtiAvailableToGetNonce() throws Exception {
        dPoPNonceProvider.clear();
        dPoPNonceProvider.setNonce("get-nonce-nonce");
        try (final WebClient webClient = createWebClient()) {
            // A proof carrying a jti but no nonce triggers the 'use_dpop_nonce' challenge, which calls getNonce.
            TextPage textPage = loginAndClick(webClient, "login-jwt-no-nonce-with-jti/get-nonce-jti");
            assertEquals("401 status from ProtectedResource", textPage.getContent());

            String wwwAuthenticate = textPage.getWebResponse()
                    .getResponseHeaderValue(HttpHeaderNames.WWW_AUTHENTICATE.toString());
            assertNotNull(wwwAuthenticate);
            assertTrue(wwwAuthenticate.contains("DPoP error=\"use_dpop_nonce\""),
                    () -> "Expected 'DPoP error=\"use_dpop_nonce\"', but got: " + wwwAuthenticate);
            assertEquals("get-nonce-nonce", textPage.getWebResponse().getResponseHeaderValue(OidcConstants.DPOP_NONCE));

            assertEquals("get-nonce-jti", dPoPNonceProvider.getLastGetNonceJti());
            webClient.getCookieManager().clearCookies();
        } finally {
            dPoPNonceProvider.clear();
        }
    }

    private void assertProofAccepted(String nonce, String jti) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            TextPage textPage = loginAndClick(webClient, "login-jwt-with-nonce-and-jti/" + nonce + "/" + jti);
            assertEquals("Hello, alice; JWK thumbprint in JWT: true, JWK thumbprint in introspection: false",
                    textPage.getContent());
            webClient.getCookieManager().clearCookies();
        }
    }

    private void assertProofReplayRejected(String nonce, String jti) throws Exception {
        try (final WebClient webClient = createWebClient()) {
            TextPage textPage = loginAndClick(webClient, "login-jwt-with-nonce-and-jti/" + nonce + "/" + jti);
            assertEquals("401 status from ProtectedResource", textPage.getContent());

            String wwwAuthenticate = textPage.getWebResponse()
                    .getResponseHeaderValue(HttpHeaderNames.WWW_AUTHENTICATE.toString());
            assertNotNull(wwwAuthenticate);
            assertTrue(wwwAuthenticate.contains("DPoP error=\"use_dpop_nonce\""),
                    () -> "Expected 'DPoP error=\"use_dpop_nonce\"', but got: " + wwwAuthenticate);
            String dpopNonce = textPage.getWebResponse().getResponseHeaderValue(OidcConstants.DPOP_NONCE);
            assertEquals(nonce, dpopNonce);

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    public static final class UseJtiDPoPNonceProvider implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("use-jti-dpop-nonce-provider", "true");
        }
    }
}
