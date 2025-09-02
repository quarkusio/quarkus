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
import org.jose4j.jwk.EcJwkGenerator;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.UnresolvableKeyException;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCustomizer;
import io.smallrye.jwt.build.Jwt;

public class OidcProviderTest {

    @Test
    public void testAlgorithmCustomizer() throws Exception {

        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");

        final String token = Jwt.issuer("http://keycloak/realm").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        final String newToken = replaceAlgorithm(token, "ES256");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");
        OidcTenantConfig oidcConfig = new OidcTenantConfig();

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            try {
                provider.verifyJwtToken(newToken, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
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
            assertEquals("http://keycloak/realm", result.localVerificationResult.getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidSingleRsaJwkWithoutKid() throws Exception {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        EllipticCurveJsonWebKey ecJsonWebKey = EcJwkGenerator.generateJwk(EllipticCurves.P256);

        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "," + ecJsonWebKey.toJson() + "]}");

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaJsonWebKey.getPrivateKey());

        try (OidcProvider provider = new OidcProvider(null, new OidcTenantConfig(), jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("http://keycloak/realm", result.localVerificationResult.getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKid() throws Exception {
        RsaJsonWebKey rsaJsonWebKey1 = RsaJwkGenerator.generateJwk(2048);
        RsaJsonWebKey rsaJsonWebKey2 = RsaJwkGenerator.generateJwk(2048);
        JsonWebKeySet jwkSet = new JsonWebKeySet(
                "{\"keys\": [" + rsaJsonWebKey1.toJson() + "," + rsaJsonWebKey2.toJson() + "]}");

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaJsonWebKey1.getPrivateKey());

        try (OidcProvider provider = new OidcProvider(null, new OidcTenantConfig(), jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getCause() instanceof UnresolvableKeyException);
            }
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKidTryAll() throws Exception {
        RsaJsonWebKey rsaJsonWebKey1 = RsaJwkGenerator.generateJwk(2048);
        RsaJsonWebKey rsaJsonWebKey2 = RsaJwkGenerator.generateJwk(2048);
        JsonWebKeySet jwkSet = new JsonWebKeySet(
                "{\"keys\": [" + rsaJsonWebKey1.toJson() + "," + rsaJsonWebKey2.toJson() + "]}");

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaJsonWebKey2.getPrivateKey());
        final OidcTenantConfig config = new OidcTenantConfig();
        config.jwks.tryAll = true;

        try (OidcProvider provider = new OidcProvider(null, config, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("http://keycloak/realm", result.localVerificationResult.getString("iss"));
        }
    }

    @Test
    public void testTokenWithoutKidMultipleRSAJwkWithoutKidTryAllNoMatching() throws Exception {
        RsaJsonWebKey rsaJsonWebKey1 = RsaJwkGenerator.generateJwk(2048);
        RsaJsonWebKey rsaJsonWebKey2 = RsaJwkGenerator.generateJwk(2048);
        RsaJsonWebKey rsaJsonWebKey3 = RsaJwkGenerator.generateJwk(2048);
        JsonWebKeySet jwkSet = new JsonWebKeySet(
                "{\"keys\": [" + rsaJsonWebKey1.toJson() + "," + rsaJsonWebKey2.toJson() + "]}");

        final String token = Jwt.issuer("http://keycloak/realm").sign(rsaJsonWebKey3.getPrivateKey());
        final OidcTenantConfig config = new OidcTenantConfig();
        config.jwks.tryAll = true;

        try (OidcProvider provider = new OidcProvider(null, config, jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
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
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.token.subjectRequired = true;

        final String tokenWithSub = Jwt.subject("subject").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithSub, false, true, null);
            assertEquals("subject", result.localVerificationResult.getString(Claims.sub.name()));
        }

        final String tokenWithoutSub = Jwt.claims().jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
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

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(tokenWithNonce, false, false, "123456");
            assertEquals("123456", result.localVerificationResult.getString(Claims.nonce.name()));
        }

        final String tokenWithoutNonce = Jwt.claims().jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            try {
                provider.verifyJwtToken(tokenWithoutNonce, false, false, "123456");
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("claim nonce is missing"));
            }
        }
    }

    @Test
    public void testAge() throws Exception {
        String tokenPayload = "{\n" +
                "  \"exp\":  " + Instant.now().plusSeconds(1000).getEpochSecond() + "\n" +
                "}";

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(tokenPayload);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        jws.setKey(rsaJsonWebKey.getPrivateKey());

        String token = jws.getCompactSerialization();

        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.token.issuedAtRequired = false;

        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertNull(result.localVerificationResult.getString(Claims.iat.name()));
        }

        OidcTenantConfig oidcConfigRequireAge = new OidcTenantConfig();
        oidcConfigRequireAge.token.issuedAtRequired = true;

        try (OidcProvider provider = new OidcProvider(null, oidcConfigRequireAge, jwkSet)) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("No Issued At (iat) claim present."));
            }
        }
    }

    @Test
    public void testJwtValidators() throws Exception {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");
        JsonWebKeySet jwkSet = new JsonWebKeySet("{\"keys\": [" + rsaJsonWebKey.toJson() + "]}");

        OidcTenantConfig oidcConfig = new OidcTenantConfig();

        String token = Jwt.claim("claim1", "claimValue1").claim("claim2", "claimValue2").jws().keyId("k1")
                .sign(rsaJsonWebKey.getPrivateKey());

        // no validators
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, null)) {
            TokenVerificationResult result = provider.verifyJwtToken(token, false, false, null);
            assertEquals("claimValue1", result.localVerificationResult.getString("claim1"));
            assertEquals("claimValue2", result.localVerificationResult.getString("claim2"));
        }

        // one validator
        Validator validator1 = new Validator() {
            @Override
            public String validate(JwtContext jwtContext) throws MalformedClaimException {
                if (jwtContext.getJwtClaims().hasClaim("claim1")) {
                    return "Claim1 is not allowed!";
                }
                return null;
            }
        };
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("Claim1 is not allowed!"));
            }
        }

        // two validators
        Validator validator2 = new Validator() {
            @Override
            public String validate(JwtContext jwtContext) throws MalformedClaimException {
                if (jwtContext.getJwtClaims().hasClaim("claim2")) {
                    return "Claim2 is not allowed!";
                }
                return null;
            }
        };
        // check the first validator is still run
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1, validator2))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("Claim1 is not allowed!"));
            }
        }
        // check the second validator is applied
        token = Jwt.claim("claim2", "claimValue2").jws().keyId("k1").sign(rsaJsonWebKey.getPrivateKey());
        try (OidcProvider provider = new OidcProvider(null, oidcConfig, jwkSet, null, List.of(validator1, validator2))) {
            try {
                provider.verifyJwtToken(token, false, false, null);
                fail("InvalidJwtException expected");
            } catch (InvalidJwtException ex) {
                assertTrue(ex.getMessage().contains("Claim2 is not allowed!"));
            }
        }
    }

}
