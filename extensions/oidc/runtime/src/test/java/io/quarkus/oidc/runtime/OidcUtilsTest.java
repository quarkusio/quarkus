package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OIDCException;
import io.vertx.core.json.JsonObject;

public class OidcUtilsTest {

    @Test
    public void testTokenWithCorrectIssuer() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromIssuer("https://server.example.com");
        InputStream is = getClass().getResourceAsStream("/tokenIssuer.json");
        assertTrue(OidcUtils.validateClaims(tokenClaims, read(is)));
    }

    @Test
    public void testTokenWithWrongIssuer() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromIssuer("https://servers.example.com");
        InputStream is = getClass().getResourceAsStream("/tokenIssuer.json");
        try {
            OidcUtils.validateClaims(tokenClaims, read(is));
            fail("Exception expected: wrong issuer");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testTokenWithCorrectStringAudience() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromAudience("https://quarkus.example.com");
        InputStream is = getClass().getResourceAsStream("/tokenStringAudience.json");
        assertTrue(OidcUtils.validateClaims(tokenClaims, read(is)));
    }

    @Test
    public void testTokenWithWrongStringAudience() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromIssuer("https://quarkus.examples.com");
        InputStream is = getClass().getResourceAsStream("/tokenStringAudience.json");
        try {
            OidcUtils.validateClaims(tokenClaims, read(is));
            fail("Exception expected: wrong audience");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testTokenWithCorrectArrayAudience() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromAudience("https://quarkus.example.com",
                "frontend_client_id");
        InputStream is = getClass().getResourceAsStream("/tokenArrayAudience.json");
        assertTrue(OidcUtils.validateClaims(tokenClaims, read(is)));
    }

    @Test
    public void testTokenWithWrongArrayAudience() throws Exception {
        OidcTenantConfig.Token tokenClaims = OidcTenantConfig.Token.fromAudience("service_client_id");
        InputStream is = getClass().getResourceAsStream("/tokenArrayAudience.json");
        try {
            OidcUtils.validateClaims(tokenClaims, read(is));
            fail("Exception expected: wrong array audience");
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
        try {
            OidcUtils.findRoles(null, rolesCfg, read(is));
            fail("Exception expected at the wrong path");
        } catch (Exception ex) {
            // expected
        }
    }

    public static JsonObject read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new JsonObject(buffer.lines().collect(Collectors.joining("\n")));
        }
    }

}
