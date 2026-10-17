package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCustomizer;
import io.smallrye.jwk.EcCurve;
import io.smallrye.jwk.EcJsonWebKey;
import io.smallrye.jwk.RsaJsonWebKey;
import io.smallrye.jws.JwsSigner;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.auth.InvalidJWTException;
import io.smallrye.jwt.auth.UnresolvableKeyException;
import io.smallrye.jwt.build.Jwt;

public class OidcProviderTest {

    @Test
    public void testAlgorithmCustomizer() throws Exception {

        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).keyId("k1").build();

        final String token = Jwt.issuer("http://keycloak/realm").jws().keyId("k1").sign(rsaKey.privateKey());
        final String newToken = replaceAlgorithm(token, "ES256");
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey).asJsonString());
        OidcTenantConfig oidcConfig = new OidcTenantConfig();

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            try {
                provider.verifyJwtToken(newToken, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                // continue
            }
        }

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, new TokenCustomizer() {

            @Override
            public JsonObject customizeHeaders(JsonObject headers) {
                return Json.createObjectBuilder(headers).add("alg", "RS256").build();
            }

        }, null)) {
            TokenVerificationResult result = provider.verifyJwtToken(newToken, false, false, null);
            assertEquals("http://keycloak/realm", result.localVerificationResult().getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidSingleRsaJwkWithoutKid() throws Exception {
        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).build();
        EcJsonWebKey ecKey = EcJsonWebKey.builder(EcCurve.P_256).build();

        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey, ecKey).asJsonString());

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaKey.privateKey());

        try (OidcProvider provider = new OidcProvider(null, new OidcTenantConfig(), jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("http://keycloak/realm", result.localVerificationResult().getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKid() throws Exception {
        RsaJsonWebKey rsaKey1 = RsaJsonWebKey.builder(2048).build();
        RsaJsonWebKey rsaKey2 = RsaJsonWebKey.builder(2048).build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey1, rsaKey2).asJsonString());

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaKey1.privateKey());

        try (OidcProvider provider = new OidcProvider(null, new OidcTenantConfig(), jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getCause() instanceof UnresolvableKeyException);
            }
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKidTryAll() throws Exception {
        RsaJsonWebKey rsaKey1 = RsaJsonWebKey.builder(2048).build();
        RsaJsonWebKey rsaKey2 = RsaJsonWebKey.builder(2048).build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey1, rsaKey2).asJsonString());

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaKey2.privateKey());
        final OidcTenantConfig config = new OidcTenantConfig();
        config.jwks.tryAll = true;

        try (OidcProvider provider = new OidcProvider(null, config, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("http://keycloak/realm", result.localVerificationResult().getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKidTryAllNoMatching() throws Exception {
        RsaJsonWebKey rsaKey1 = RsaJsonWebKey.builder(2048).build();
        RsaJsonWebKey rsaKey2 = RsaJsonWebKey.builder(2048).build();
        RsaJsonWebKey rsaKey3 = RsaJsonWebKey.builder(2048).build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey1, rsaKey2).asJsonString());

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaKey3.privateKey());
        final OidcTenantConfig config = new OidcTenantConfig();
        config.jwks.tryAll = true;

        try (OidcProvider provider = new OidcProvider(null, config, jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getCause() instanceof UnresolvableKeyException);
            }
        }
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
        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).keyId("k1").build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey).asJsonString());

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.token.subjectRequired = true;

        final String tokenWithSub = Jwt.subject("subject").jws().keyId("k1").sign(rsaKey.privateKey());

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithSub, false, true, null);
            assertEquals("subject", result.localVerificationResult().getString(Claims.sub.name()));
        }

        final String tokenWithoutSub = Jwt.claims().jws().keyId("k1").sign(rsaKey.privateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            try {
                provider.verifyJwtToken(tokenWithoutSub, false, true, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("sub"));
            }
        }
    }

    @Test
    public void testNonce() throws Exception {
        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).keyId("k1").build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey).asJsonString());

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.authentication.nonceRequired = true;

        final String tokenWithNonce = Jwt.claim("nonce", "123456").jws().keyId("k1").sign(rsaKey.privateKey());

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithNonce, false, false, "123456");
            assertEquals("123456", result.localVerificationResult().getString(Claims.nonce.name()));
        }

        final String tokenWithoutNonce = Jwt.claims().jws().keyId("k1").sign(rsaKey.privateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            try {
                provider.verifyJwtToken(tokenWithoutNonce, false, false, "123456");
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("nonce"));
            }
        }
    }

    @Test
    public void testAge() throws Exception {
        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).build();

        // the raw payload is signed so that the token has 'exp' but no 'iat' claim
        String tokenPayload = "{\"exp\": " + Instant.now().plusSeconds(1000).getEpochSecond() + "}";
        String token = JwsSigner.builder(rsaKey.privateKey()).algorithm(SignatureAlgorithm.RS256).build()
                .sign(tokenPayload);

        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey).asJsonString());

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.token.issuedAtRequired = false;

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertNull(result.localVerificationResult().getString(Claims.iat.name()));
        }

        OidcTenantConfig oidcConfigRequireAge = new OidcTenantConfig();
        oidcConfigRequireAge.token.issuedAtRequired = true;

        try (OidcProvider provider = new OidcProvider(null, oidcConfigRequireAge, jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("iat"));
            }
        }
    }

    @Test
    public void testJwtValidators() throws Exception {
        RsaJsonWebKey rsaKey = RsaJsonWebKey.builder(2048).keyId("k1").build();
        JsonWebKeySet jwkSet = new JsonWebKeySet(io.smallrye.jwk.JsonWebKeySet.of(rsaKey).asJsonString());

        OidcTenantConfig oidcConfig = new OidcTenantConfig();

        String token = Jwt.claim("claim1", "claimValue1").claim("claim2", "claimValue2").jws().keyId("k1")
                .sign(rsaKey.privateKey());

        // no validators
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("claimValue1", result.localVerificationResult().getString("claim1"));
            assertEquals("claimValue2", result.localVerificationResult().getString("claim2"));
        }

        // one validator
        ClaimsValidator validator1 = new ClaimsValidator() {
            @Override
            public String validate(VerificationContext context) {
                if (context.claims().hasClaim("claim1")) {
                    return "Claim1 is not allowed!";
                }
                return null;
            }
        };
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("Claim1 is not allowed!"));
            }
        }

        // two validators
        ClaimsValidator validator2 = new ClaimsValidator() {
            @Override
            public String validate(VerificationContext context) {
                if (context.claims().hasClaim("claim2")) {
                    return "Claim2 is not allowed!";
                }
                return null;
            }
        };
        // check the first validator is still run
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1, validator2))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("Claim1 is not allowed!"));
            }
        }
        // check the second validator is applied
        token = Jwt.claim("claim2", "claimValue2").jws().keyId("k1").sign(rsaKey.privateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1, validator2))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJWTException expected");
            } catch (InvalidJWTException ex) {
                assertTrue(ex.getMessage().contains("Claim2 is not allowed!"));
            }
        }
    }

}
