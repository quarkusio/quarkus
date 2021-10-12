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
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.json.JsonObject;

public class OidcUtilsTest {

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
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath("application_card/embedded/roles");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r1"));
        assertTrue(roles.contains("r2"));
    }

    @Test
    public void testTokenWithCustomNamespacedRoles() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath("application_card/embedded/\"https://custom/roles\"");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r3"));
        assertTrue(roles.contains("r4"));
    }

    @Test
    public void testTokenWithScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath("scope");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPathAndSeparator("customScope", ",");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenCustomScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomRolesWrongPath() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath("application-card/embedded/roles");
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

    public static JsonObject read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new JsonObject(buffer.lines().collect(Collectors.joining("\n")));
        }
    }

}
