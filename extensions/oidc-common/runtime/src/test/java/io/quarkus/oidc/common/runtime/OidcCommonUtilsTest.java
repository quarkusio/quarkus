package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Optional;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;

public class OidcCommonUtilsTest {

    @Test
    public void testProxyOptionsWithHostWithoutScheme() throws Exception {
        OidcCommonConfig.Proxy config = new OidcCommonConfig.Proxy();
        config.host = Optional.of("localhost");
        config.port = 8080;
        config.username = Optional.of("user");
        config.password = Optional.of("password");

        ProxyOptions options = OidcCommonUtils.toProxyOptions(config).get();
        assertEquals("localhost", options.getHost());
        assertEquals(8080, options.getPort());
        assertEquals("user", options.getUsername());
        assertEquals("password", options.getPassword());
    }

    @Test
    public void testProxyOptionsWithHostWithScheme() throws Exception {
        OidcCommonConfig.Proxy config = new OidcCommonConfig.Proxy();
        config.host = Optional.of("http://localhost");
        config.port = 8080;
        config.username = Optional.of("user");
        config.password = Optional.of("password");

        assertEquals("http", URI.create(config.host.get()).getScheme());

        ProxyOptions options = OidcCommonUtils.toProxyOptions(config).get();
        assertEquals("localhost", options.getHost());
        assertEquals(8080, options.getPort());
        assertEquals("user", options.getUsername());
        assertEquals("password", options.getPassword());
    }

    @Test
    public void testJwtTokenWithScope() throws Exception {
        OidcClientCommonConfig cfg = new OidcClientCommonConfig() {
        };
        cfg.setClientId("client");
        cfg.credentials.jwt.claims.put("scope", "read,write");
        PrivateKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String jwt = OidcCommonUtils.signJwtWithKey(cfg, "http://localhost", key);
        JsonObject json = decodeJwtContent(jwt);
        String scope = json.getString("scope");
        assertEquals("read,write", scope);
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
