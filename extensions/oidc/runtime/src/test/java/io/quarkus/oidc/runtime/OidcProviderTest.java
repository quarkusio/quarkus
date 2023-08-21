package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCustomizer;
import io.smallrye.jwt.build.Jwt;

public class OidcProviderTest {

    @SuppressWarnings("resource")
    @Test
    public void testAlgorithmCustomizer() throws Exception {

        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");

        final String token = Jwt.issuer("http://keycloak/ream").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        final String newToken = replaceAlgorithm(token, "ES256");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");
        OidcTenantConfig oidcConfig = new OidcTenantConfig();

        OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null);
        try {
            provider.verifyJwtToken(newToken, false, false, null);
            fail("InvalidJwtException expected");
        } catch (InvalidJwtException ex) {
            // continue
        }

        provider = new OidcProvider(null, oidcConfig, jwkSet, new TokenCustomizer() {

            @Override
            public JsonObject customizeHeaders(JsonObject headers) {
                return Json.createObjectBuilder(headers).add("alg", "RS256").build();
            }

        }, null);
        TokenVerificationResult result = provider.verifyJwtToken(newToken, false, false, null);
        assertEquals("http://keycloak/ream", result.localVerificationResult.getString("iss"));
    }

    private static String replaceAlgorithm(String token, String algorithm) {
        io.vertx.core.json.JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        headers.put("alg", algorithm);
        String newHeaders = new String(
                Base64.getUrlEncoder().withoutPadding().encode(headers.toString().getBytes()),
                StandardCharsets.UTF_8);
        int dotIndex = token.indexOf('.');
        return newHeaders + token.substring(dotIndex);
    }

    @Test
    public void testSubject() throws Exception {

        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.token.subjectRequired = true;

        final String tokenWithSub = Jwt.subject("subject").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithSub, false, true, null);
            assertEquals("subject", result.localVerificationResult.getString(Claims.sub.name()));
        }

        final String tokenWithoutSub = Jwt.claims().jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            try {
                provider.verifyJwtToken(tokenWithoutSub, false, true, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("No Subject (sub) claim is present"));
            }
        }
    }

    @Test
    public void testNonce() throws Exception {

        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.authentication.nonceRequired = true;

        final String tokenWithNonce = Jwt.claim("nonce", "123456").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithNonce, false, false, "123456");
            assertEquals("123456", result.localVerificationResult.getString(Claims.nonce.name()));
        }

        final String tokenWithoutNonce = Jwt.claims().jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            try {
                provider.verifyJwtToken(tokenWithoutNonce, false, false, "123456");
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("claim nonce is missing"));
            }
        }
    }
}
