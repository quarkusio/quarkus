package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonObject;

public class OidcUtilsTest {

    @Test
    public void testGetSingleSessionCookie() throws Exception {

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.setTenantId("test");
        Map<String, Object> context = new HashMap<>();
        String sessionCookieValue = OidcUtils.getSessionCookie(context,
                Map.of("q_session_test", new CookieImpl("q_session_test", "tokens")), oidcConfig);
        assertEquals("tokens", sessionCookieValue);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<String> names = (List) context.get(OidcUtils.SESSION_COOKIE_NAME);
        assertEquals(1, names.size());
        assertEquals("q_session_test", names.get(0));
    }

    @Test
    public void testGetMultipleSessionCookies() throws Exception {

        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.setTenantId("test");

        char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        StringBuilder expectedCookieValue = new StringBuilder();
        Map<String, Cookie> cookies = new HashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            char[] data = new char[OidcUtils.MAX_COOKIE_VALUE_LENGTH];
            Arrays.fill(data, alphabet[i]);
            String cookieName = "q_session_test_chunk_" + (i + 1);
            String nextChunk = new String(data);
            expectedCookieValue.append(nextChunk);
            cookies.put(cookieName, new CookieImpl(cookieName, nextChunk));
        }
        String lastChunk = String.valueOf("tokens");
        expectedCookieValue.append(lastChunk);
        String lastCookieName = "q_session_test_chunk_" + (alphabet.length + 1);
        cookies.put(lastCookieName, new CookieImpl(lastCookieName, lastChunk));

        Map<String, Object> context = new HashMap<>();
        String sessionCookieValue = OidcUtils.getSessionCookie(context, cookies, oidcConfig);
        assertEquals(expectedCookieValue.toString(), sessionCookieValue);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<String> names = (List) context.get(OidcUtils.SESSION_COOKIE_NAME);
        assertEquals(alphabet.length + 1, names.size());
        for (int i = 0; i < names.size(); i++) {
            assertEquals("q_session_test_chunk_" + (i + 1), names.get(i));
        }
    }

    @Test
    public void testCorrectTokenType() throws Exception {
        OidcTenantConfig.Token tokenClaims = new OidcTenantConfig.Token();
        tokenClaims.setTokenType("access_token");
        JsonObject json = new JsonObject();
        json.put("typ", "access_token");
        OidcUtils.validatePrimaryJwtTokenType(tokenClaims, json);
    }

    @Test
    public void testWrongTokenType() throws Exception {
        OidcTenantConfig.Token tokenClaims = new OidcTenantConfig.Token();
        tokenClaims.setTokenType("access_token");
        JsonObject json = new JsonObject();
        json.put("typ", "refresh_token");
        try {
            OidcUtils.validatePrimaryJwtTokenType(tokenClaims, json);
            fail("Exception expected: wrong token type");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testKeycloakRefreshTokenType() throws Exception {
        JsonObject json = new JsonObject();
        json.put("typ", "Refresh");
        try {
            OidcUtils.validatePrimaryJwtTokenType(new OidcTenantConfig.Token(), json);
            fail("Exception expected: wrong token type");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testKeycloakRealmAccessToken() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakRealmAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("role1"));
        assertTrue(roles.contains("role2"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenClient1() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles("client1", rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("realm1"));
        assertTrue(roles.contains("role1"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenClient2() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles("client2", rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("realm1"));
        assertTrue(roles.contains("role2"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenNullClient() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(1, roles.size());
        assertTrue(roles.contains("realm1"));
    }

    @Test
    public void testTokenWithGroups() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenGroups.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("group1"));
        assertTrue(roles.contains("group2"));
    }

    @Test
    public void testTokenWithCustomRoles() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application_card/embedded/roles"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r1"));
        assertTrue(roles.contains("r2"));
    }

    @Test
    public void testTokenWithMultipleCustomRolePaths() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(List.of("application_card/embedded/roles", "application_card/embedded2/roles"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(4, roles.size());
        assertTrue(roles.contains("r1"));
        assertTrue(roles.contains("r2"));
        assertTrue(roles.contains("r5"));
        assertTrue(roles.contains("r6"));
    }

    @Test
    public void testTokenWithCustomNamespacedRoles() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application_card/embedded/\"https://custom/roles\""));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r3"));
        assertTrue(roles.contains("r4"));
    }

    @Test
    public void testTokenWithCustomNamespacedRolesWithSpaces() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList(" application_card/embedded/\"https://custom/roles\" "));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r3"));
        assertTrue(roles.contains("r4"));
    }

    @Test
    public void testTokenWithScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(Collections.singletonList("scope"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPathAndSeparator(Collections.singletonList("customScope"), ",");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenCustomScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomRolesWrongPath() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application-card/embedded/roles"));
        InputStream is = getClass().getResourceAsStream("/tokenCustomPath.json");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(is));
        assertEquals(0, roles.size());
    }

    @Test
    public void testTokenIsOpaque() throws Exception {
        assertTrue(OidcUtils.isOpaqueToken("123"));
        assertTrue(OidcUtils.isOpaqueToken("1.23"));
        assertFalse(OidcUtils.isOpaqueToken("1.2.3"));
    }

    @Test
    public void testDecodeOpaqueTokenAsJwt() throws Exception {
        assertNull(OidcUtils.decodeJwtContent("123"));
        assertNull(OidcUtils.decodeJwtContent("1.23"));
        assertNull(OidcUtils.decodeJwtContent("1.2.3"));
    }

    @Test
    public void testDecodeJwt() throws Exception {
        final byte[] keyBytes = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
                .getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");
        String jwt = Jwt.claims().sign(key);
        assertNull(OidcUtils.decodeJwtContent(jwt + ".4"));
        JsonObject json = OidcUtils.decodeJwtContent(jwt);
        assertTrue(json.containsKey("iat"));
        assertTrue(json.containsKey("exp"));
        assertTrue(json.containsKey("jti"));
    }

    @Test
    public void testTransformScopeToPermission() throws Exception {
        Permission[] perms = OidcUtils.transformScopesToPermissions(
                List.of("read", "read:d", "read:", ":read"));
        assertEquals(4, perms.length);

        assertEquals("read", perms[0].getName());
        assertNull(perms[0].getActions());
        assertEquals("read", perms[1].getName());
        assertEquals("d", perms[1].getActions());
        assertEquals("read:", perms[2].getName());
        assertNull(perms[2].getActions());
        assertEquals(":read", perms[3].getName());
        assertNull(perms[3].getActions());
    }

    @Test
    public void testEncodeScopesOpenidAdded() throws Exception {
        OidcTenantConfig config = new OidcTenantConfig();
        assertEquals("openid", OidcUtils.encodeScopes(config));
    }

    @Test
    public void testEncodeScopesOpenidNotAdded() throws Exception {
        OidcTenantConfig config = new OidcTenantConfig();
        config.authentication.setAddOpenidScope(false);
        assertEquals("", OidcUtils.encodeScopes(config));
    }

    @Test
    public void testEncodeAllScopes() throws Exception {
        OidcTenantConfig config = new OidcTenantConfig();
        config.authentication.setScopes(List.of("a:1", "b:2"));
        config.authentication.setExtraParams(Map.of("scope", "c,d"));
        assertEquals("openid+a%3A1+b%3A2+c+d", OidcUtils.encodeScopes(config));
    }

    public static JsonObject read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new JsonObject(buffer.lines().collect(Collectors.joining("\n")));
        }
    }

}
