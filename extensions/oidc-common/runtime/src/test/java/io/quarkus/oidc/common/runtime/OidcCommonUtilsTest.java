package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Optional;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public class OidcCommonUtilsTest {

    @Test
    public void testMaskAuthorizationBasicScheme() throws Exception {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().set("Authorization", "Basic base64encoded");

        MultiMap maskedHeaders = OidcCommonUtils.maskAuthorizationHeader(headers);
        assertEquals("Basic ...", maskedHeaders.get("Authorization"));

        assertEquals("Basic base64encoded", headers.get("Authorization"));
    }

    @Test
    public void testMaskAuthorizationBearerScheme() throws Exception {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().set("authorization", "Bearer token");

        MultiMap maskedHeaders = OidcCommonUtils.maskAuthorizationHeader(headers);
        assertEquals("Bearer ...", maskedHeaders.get("Authorization"));

        assertEquals("Bearer token", headers.get("Authorization"));
    }

    @Test
    public void testMaskAuthorizationWithoutScheme() throws Exception {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().set("Authorization", "API-Key");

        MultiMap maskedHeaders = OidcCommonUtils.maskAuthorizationHeader(headers);
        assertEquals("...", maskedHeaders.get("Authorization"));

        assertEquals("API-Key", headers.get("Authorization"));
    }

    @Test
    public void testMaskClientSecretFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("client_secret", "secret");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("client_secret"));

        assertEquals("secret", form.get("client_secret"));
    }

    @Test
    public void testMaskClientAssertionFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("client_assertion", "ey.token.signature");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("client_assertion"));

        assertEquals("ey.token.signature", form.get("client_assertion"));
    }

    @Test
    public void testMaskPasswordGrantPasswordFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("password", "secret");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("password"));

        assertEquals("secret", form.get("password"));
    }

    @Test
    public void testMaskRefreshTokenFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("refresh_token", "rt");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("refresh_token"));

        assertEquals("rt", form.get("refresh_token"));
    }

    @Test
    public void testMaskAuthorizationCodeFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("code", "somecode");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("code"));

        assertEquals("somecode", form.get("code"));
    }

    @Test
    public void testMaskPkceCodeVerifierFormData() throws Exception {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().set("code_verifier", "somecode");

        MultiMap maskedForm = OidcCommonUtils.maskFormData(form);
        assertEquals("...", maskedForm.get("code_verifier"));

        assertEquals("somecode", form.get("code_verifier"));
    }

    @Test
    public void testMaskJsonTokensResponse() throws Exception {
        JsonObject json = new JsonObject()
                .put("access_token", "at").put("refresh_token", "rt").put("id_token", "id");

        JsonObject maskedJson = OidcCommonUtils.maskJsonTokens(json);
        assertEquals("...", maskedJson.getString("access_token"));
        assertEquals("...", maskedJson.getString("refresh_token"));
        assertEquals("...", maskedJson.getString("id_token"));

        assertEquals("at", json.getString("access_token"));
        assertEquals("rt", json.getString("refresh_token"));
        assertEquals("id", json.getString("id_token"));

    }

    @Test
    public void testJwtTokenWithScope() throws Exception {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.claims.put("scope", "read,write");
        PrivateKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String jwt = OidcCommonUtils.signJwtWithKey(cfg, "http://some.service.com", key);
        JsonObject json = decodeJwtContent(jwt);
        String scope = json.getString("scope");
        assertEquals("read,write", scope);
        assertEquals("http://some.service.com", json.getString("aud"));
    }

    @Test
    public void testSignWithAudience() throws Exception {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.audience = Optional.of("https://server.example.com");

        PrivateKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String jwt = OidcCommonUtils.signJwtWithKey(cfg, "http://localhost", key);
        JsonObject json = decodeJwtContent(jwt);
        assertEquals("https://server.example.com", json.getString("aud"));
    }

    @Test
    public void testSignWithAudienceRemoveTrailingSlash() throws Exception {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.audience = Optional.of("https://server.example.com/");

        PrivateKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String jwt = OidcCommonUtils.signJwtWithKey(cfg, "http://localhost", key);
        JsonObject json = decodeJwtContent(jwt);
        assertEquals("https://server.example.com", json.getString("aud"));
    }

    @Test
    public void testSignWithAudienceKeepTrailingSlash() throws Exception {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.audience = Optional.of("https://server.example.com/");
        cfg.credentials.jwt.keepAudienceTrailingSlash = true;

        PrivateKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String jwt = OidcCommonUtils.signJwtWithKey(cfg, "http://localhost", key);
        JsonObject json = decodeJwtContent(jwt);
        assertEquals("https://server.example.com/", json.getString("aud"));
    }

    @Test
    public void testSecretAndClientSecretAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.secret = Optional.of("secret1");
        cfg.credentials.clientSecret.value = Optional.of("secret2");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("mutually exclusive"));
    }

    @Test
    public void testClientSecretValueAndJwtSecretAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    public void testSecretAndJwtSecretProviderAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.secret = Optional.of("secret");
        cfg.credentials.jwt.secretProvider.key = Optional.of("vault-jwt-secret");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    public void testClientSecretValueAndJwtSecretProviderAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        cfg.credentials.jwt.secretProvider.key = Optional.of("vault-jwt-secret");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    public void testClientSecretProviderAndJwtSecretAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.provider.key = Optional.of("vault-key");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    public void testClientSecretProviderAndJwtSecretProviderAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.provider.key = Optional.of("vault-key");
        cfg.credentials.jwt.secretProvider.key = Optional.of("vault-jwt-secret");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    public void testClientSecretAndJwtKeyFileAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT key"));
    }

    @Test
    public void testClientSecretAndJwtKeyAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.secret = Optional.of("secret");
        cfg.credentials.jwt.key = Optional.of("pem-key-content");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT key"));
    }

    @Test
    public void testClientSecretAndJwtKeyStoreAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT key"));
    }

    @Test
    public void testClientSecretProviderAndJwtKeyFileAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.provider.key = Optional.of("vault-key");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("JWT key"));
    }

    @Test
    public void testJwtKeyAndKeyFileAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.key = Optional.of("pem-key-content");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("credentials.jwt.key"));
        assertTrue(ex.getMessage().contains("credentials.jwt.key-file"));
    }

    @Test
    public void testJwtKeyAndKeyStoreAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.key = Optional.of("pem-key-content");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("credentials.jwt.key"));
        assertTrue(ex.getMessage().contains("credentials.jwt.key-store-file"));
    }

    @Test
    public void testJwtKeyFileAndKeyStoreAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("credentials.jwt.key-file"));
        assertTrue(ex.getMessage().contains("credentials.jwt.key-store-file"));
    }

    @Test
    public void testAllThreeJwtKeyPropertiesAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.key = Optional.of("pem-key-content");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("credentials.jwt.key"));
    }

    @Test
    public void testJwtSecretAndJwtKeyAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");
        cfg.credentials.jwt.key = Optional.of("pem-key-content");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("JWT private key"));
    }

    @Test
    public void testJwtSecretAndJwtKeyFileAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("JWT private key"));
    }

    @Test
    public void testJwtSecretProviderAndJwtKeyStoreAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secretProvider.key = Optional.of("vault-jwt-secret");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("JWT private key"));
    }

    @Test
    public void testClientSecretAndJwtBearerAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.BEARER;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("jwt.source=bearer"));
    }

    @Test
    public void testClientSecretAndJwtSpiffeAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.secret = Optional.of("secret");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.SPIFFE_JWT;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("client secret"));
        assertTrue(ex.getMessage().contains("jwt.source=spiffe_jwt"));
    }

    @Test
    public void testJwtKeyFileAndJwtBearerAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.BEARER;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT private key"));
        assertTrue(ex.getMessage().contains("jwt.source=bearer"));
    }

    @Test
    public void testJwtKeyStoreAndJwtSpiffeAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.keyStoreFile = Optional.of("keystore.jks");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.SPIFFE_JWT;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT private key"));
        assertTrue(ex.getMessage().contains("jwt.source=spiffe_jwt"));
    }

    @Test
    public void testJwtSecretAndJwtBearerAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.BEARER;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("jwt.source=bearer"));
    }

    @Test
    public void testJwtSecretProviderAndJwtSpiffeAreMutuallyExclusive() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secretProvider.key = Optional.of("vault-jwt-secret");
        cfg.credentials.jwt.source = OidcClientCommonConfig.Credentials.Jwt.Source.SPIFFE_JWT;

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> OidcCommonUtils.verifyCommonConfiguration(cfg, false, false));
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("jwt.source=spiffe_jwt"));
    }

    @Test
    public void testSingleClientSecretIsValid() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.clientSecret.value = Optional.of("secret");
        OidcCommonUtils.verifyCommonConfiguration(cfg, false, false);
    }

    @Test
    public void testSingleJwtKeyFileIsValid() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.keyFile = Optional.of("privateKey.pem");
        OidcCommonUtils.verifyCommonConfiguration(cfg, false, false);
    }

    @Test
    public void testSingleJwtSecretIsValid() {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.secret = Optional.of("jwt-secret");
        OidcCommonUtils.verifyCommonConfiguration(cfg, false, false);
    }

    public static JsonObject decodeJwtContent(String jwt) {
        String encodedContent = getJwtContentPart(jwt);
        if (encodedContent == null) {
            return null;
        }
        return decodeAsJsonObject(encodedContent);
    }

    public static String getJwtContentPart(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // let's check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        return encodedContent;
    }

    @Test
    public void testDecodeJwtContentWithNoParts() {
        // An empty or delimiter-only bearer token has zero StringTokenizer tokens.
        // These used to throw NoSuchElementException out of getJwtContentPart, which
        // escaped BearerAuthenticationMechanism as a non-AuthenticationFailedException
        // and surfaced as HTTP 500 instead of a 401 challenge.
        assertNull(OidcCommonUtils.decodeJwtContent(""));
        assertNull(OidcCommonUtils.decodeJwtContent("."));
        assertNull(OidcCommonUtils.decodeJwtContent(".."));
        assertNull(OidcCommonUtils.decodeJwtContent("..."));
        assertNull(OidcCommonUtils.decodeJwtContent("...."));
    }

    @Test
    public void testGetJwtContentPartWithNoParts() {
        assertNull(OidcCommonUtils.getJwtContentPart(""));
        assertNull(OidcCommonUtils.getJwtContentPart("."));
        assertNull(OidcCommonUtils.getJwtContentPart(".."));
        assertNull(OidcCommonUtils.getJwtContentPart("..."));
    }

    @Test
    public void testGetJwtContentPartStillRejectsWrongPartCounts() {
        // Regression guard: the new zero-token check must not change how tokens
        // with the wrong number of parts are treated.
        assertNull(OidcCommonUtils.getJwtContentPart("onlyonepart"));
        assertNull(OidcCommonUtils.getJwtContentPart("two.parts"));
        assertNull(OidcCommonUtils.getJwtContentPart("a.b.c.d"));
        assertEquals("b", OidcCommonUtils.getJwtContentPart("a.b.c"));
    }

    private static JsonObject decodeAsJsonObject(String encodedContent) {
        try {
            return new JsonObject(base64UrlDecode(encodedContent));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String base64UrlDecode(String encodedContent) {
        return new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8);
    }
}
