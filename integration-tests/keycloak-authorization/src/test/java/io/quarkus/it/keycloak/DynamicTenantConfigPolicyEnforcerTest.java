package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode.DISABLED;
import static org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode.ENFORCING;
import static org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode.PERMISSIVE;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.quarkus.runtime.util.StringUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(KeycloakLifecycleManager.class)
@TestProfile(DynamicTenantConfigPolicyEnforcerTest.DynamicTenantConfigResolverProfile.class)
public class DynamicTenantConfigPolicyEnforcerTest extends AbstractPolicyEnforcerTest {

    @Inject
    KeycloakPolicyEnforcerConfig enforcerConfig;

    public static class DynamicTenantConfigResolverProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "dynamic-config-resolver";
        }
    }

    @Test
    public void testDynamicConfigPermissionScopes() {
        // 'jdoe' has scope 'read' and 'read' is required
        assureGetPath("/api/permission/scopes/dynamic-way", 200, getAccessToken("jdoe"), "read");
        assureGetPath("//api/permission/scopes/dynamic-way", 200, getAccessToken("jdoe"), "read");

        // 'jdoe' has scope 'read' while 'write' is required
        assureGetPath("/api/permission/scopes/dynamic-way-denied", 403, getAccessToken("jdoe"), null);
        assureGetPath("//api/permission/scopes/dynamic-way-denied", 403, getAccessToken("jdoe"), null);
    }

    @Test
    public void testDynamicConfigUserHasAdminRoleServiceTenant() {
        assureGetPath("/dynamic-permission-tenant", 403, getAccessToken("alice"), null);
        assureGetPath("//dynamic-permission-tenant", 403, getAccessToken("alice"), null);

        assureGetPath("/dynamic-permission-tenant", 403, getAccessToken("jdoe"), null);
        assureGetPath("//dynamic-permission-tenant", 403, getAccessToken("jdoe"), null);

        assureGetPath("/dynamic-permission-tenant", 200, getAccessToken("admin"), "Permission Resource Tenant");
        assureGetPath("//dynamic-permission-tenant", 200, getAccessToken("admin"), "Permission Resource Tenant");
    }

    @Test
    public void testKeycloakPolicyEnforcerTenantConfigBuilder() {
        assertBuilderPopulatedWithDefaultValues();
        assertEveryConfigPropertyCanBeSet();
        assertTenantConfigEnhanced(enforcerConfig.namedTenants().get("api-permission-tenant"));
        assertBuilderShortcuts();
        assertPathCacheConfigOnly();
        assertClaimInformationPointConfigOnly();
        assertMethodConfigOnly();
    }

    private static void assertBuilderPopulatedWithDefaultValues() {
        var config = KeycloakPolicyEnforcerTenantConfig.builder().build();
        assertEquals(20, config.connectionPoolSize());
        assertTrue(config.policyEnforcer().lazyLoadPaths());
        assertEquals(ENFORCING, config.policyEnforcer().enforcementMode());
        assertTrue(config.policyEnforcer().paths().isEmpty());
        assertFalse(config.policyEnforcer().httpMethodAsScope());
        assertEquals(30000, config.policyEnforcer().pathCache().lifespan());
        assertEquals(1000, config.policyEnforcer().pathCache().maxEntries());
        assertTrue(config.policyEnforcer().claimInformationPoint().simpleConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().complexConfig().isEmpty());
        // now let's create path and see that all defaults are populated
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("/api/one").method("TRACE", null)
                .paths("/api/two").method("TRACE", null)
                .build();
        config.policyEnforcer().paths().values().forEach(p -> {
            assertEquals(ENFORCING, p.enforcementMode());
            assertTrue(p.claimInformationPoint().complexConfig().isEmpty());
            assertTrue(p.claimInformationPoint().simpleConfig().isEmpty());
            assertTrue(p.name().isEmpty());
            assertEquals(1, p.paths().get().size());
            assertEquals(1, p.methods().size());
            var method = p.methods().get("trace");
            assertNotNull(method);
            // we didn't add scopes, and we are yet to require them
            assertTrue(method.scopes().isEmpty());
            assertEquals("TRACE", method.method());
            // default scope enforcement is ALL
            assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.ALL, method.scopesEnforcementMode());
        });
        // create config from previous config and alter one of paths
        var builder = KeycloakPolicyEnforcerTenantConfig.builder(config);
        var path = builder.paths("/api/one");
        path.permissionName("New Permission");
        path.claimInformationPoint(Map.of("key", Map.of("sub-key", "sub-value")));
        path.get(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, "scope5").build();
        config = builder.build();
        // this path must be same as the before, we didn't change it
        var p = config.policyEnforcer().paths().get("/api/two");
        assertNotNull(p);
        assertEquals(ENFORCING, p.enforcementMode());
        assertTrue(p.claimInformationPoint().complexConfig().isEmpty());
        assertTrue(p.claimInformationPoint().simpleConfig().isEmpty());
        assertTrue(p.name().isEmpty());
        assertEquals(1, p.paths().get().size());
        assertEquals(1, p.methods().size());
        var method = p.methods().get("trace");
        assertNotNull(method);
        // we didn't add scopes, and we are yet to require them
        assertTrue(method.scopes().isEmpty());
        assertEquals("TRACE", method.method());
        // default scope enforcement is ALL
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.ALL, method.scopesEnforcementMode());
        // this path must now have new method GET, scope 'scope5', simple config, new permission name and enforcement
        // TRACE method was removed, we need to assert everything else is as it was
        p = config.policyEnforcer().paths().get("/api/one");
        assertNotNull(p);
        assertEquals(ENFORCING, p.enforcementMode());
        assertTrue(p.claimInformationPoint().complexConfig().isEmpty());
        assertFalse(p.claimInformationPoint().simpleConfig().isEmpty());
        assertEquals(1, p.claimInformationPoint().simpleConfig().size());
        assertFalse(p.claimInformationPoint().simpleConfig().get("key").isEmpty());
        assertEquals("sub-value", p.claimInformationPoint().simpleConfig().get("key").get("sub-key"));
        assertFalse(p.name().isEmpty());
        assertEquals("New Permission", p.name().get());
        assertEquals(1, p.paths().get().size());
        assertEquals("/api/one", p.paths().get().get(0));
        assertEquals(2, p.methods().size());
        method = p.methods().get("trace");
        assertNotNull(method);
        method = p.methods().get("get");
        // we didn't add scopes, and we are yet to require them
        assertTrue(method.scopes().contains("scope5"));
        assertEquals("GET", method.method());
        // default scope enforcement is ALL
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, method.scopesEnforcementMode());
        // in addition to the GET and PUT and POST methods
        builder = KeycloakPolicyEnforcerTenantConfig.builder(config);
        path = builder.paths(PERMISSIVE, "/api/one");
        path.post("1", "2");
        path.put(PolicyEnforcerConfig.ScopeEnforcementMode.ANY, "3", "4");
        config = builder.build();
        var pathConfig = config.policyEnforcer().paths().get("/api/one");
        assertEquals(4, pathConfig.methods().size());
        assertEquals(PERMISSIVE, pathConfig.enforcementMode());
        var putMethod = pathConfig.methods().get("put");
        assertNotNull(putMethod);
        assertTrue(putMethod.scopes().contains("3"));
        assertTrue(putMethod.scopes().contains("4"));
        assertEquals("PUT", putMethod.method());
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.ANY, putMethod.scopesEnforcementMode());
        var postMethod = pathConfig.methods().get("post");
        assertNotNull(postMethod);
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.ALL, postMethod.scopesEnforcementMode());
        assertEquals("POST", postMethod.method());
        assertTrue(postMethod.scopes().contains("1"));
        assertTrue(postMethod.scopes().contains("2"));
        // test config with multiple path patterns
        config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/x", "/y").enforcementMode(PERMISSIVE).build();
        pathConfig = config.policyEnforcer().paths().entrySet().stream().findAny().get().getValue();
        assertEquals(2, pathConfig.paths().get().size());
        assertTrue(pathConfig.paths().get().contains("/x"));
        assertTrue(pathConfig.paths().get().contains("/y"));
        assertEquals(PERMISSIVE, pathConfig.enforcementMode());
    }

    private static void assertEveryConfigPropertyCanBeSet() {
        var builder = KeycloakPolicyEnforcerTenantConfig.builder()
                .enforcementMode(DISABLED)
                .claimInformationPoint(Map.of("one", Map.of("two", "three")), Map.of("four", Map.of()))
                .connectionPoolSize(-1)
                .lazyLoadPaths(false)
                .pathCache(5, 2)
                .httpMethodAsScope(true);
        var pathBuilder = builder.namedPaths("p1", PERMISSIVE, "path");
        pathBuilder.permissionName("n1");
        pathBuilder.method("method1", PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, "scope1", "scopes2");
        var config = builder.build();
        assertEquals(DISABLED, config.policyEnforcer().enforcementMode());
        assertEquals(-1, config.connectionPoolSize());
        assertFalse(config.policyEnforcer().lazyLoadPaths());
        assertTrue(config.policyEnforcer().httpMethodAsScope());
        assertEquals("three", config.policyEnforcer().claimInformationPoint().simpleConfig().get("one").get("two"));
        assertTrue(config.policyEnforcer().claimInformationPoint().complexConfig().get("four").isEmpty());
        assertEquals(5, config.policyEnforcer().pathCache().maxEntries());
        assertEquals(2, config.policyEnforcer().pathCache().lifespan());
        var path = config.policyEnforcer().paths().get("p1");
        assertEquals("n1", path.name().orElse(null));
        assertEquals("path", path.paths().orElse(List.of()).get(0));
        assertEquals(PERMISSIVE, path.enforcementMode());
        var method = path.methods().get("method1");
        assertTrue(method.scopes().contains("scopes2"));
        assertTrue(method.scopes().contains("scope1"));
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, method.scopesEnforcementMode());
    }

    private static void assertTenantConfigEnhanced(KeycloakPolicyEnforcerTenantConfig originalConfig) {
        var originalPath = originalConfig.policyEnforcer().paths().get("2");
        assertEquals(DISABLED, originalPath.enforcementMode());
        assertEquals("/dynamic-permission-tenant", originalPath.paths().orElse(List.of()).get(0));
        assertNull(originalConfig.policyEnforcer().paths().get("3"));
        var builder = KeycloakPolicyEnforcerTenantConfig.builder(originalConfig);
        builder.namedPaths("2").enforcementMode(ENFORCING);
        var pathBuilder = builder.namedPaths("3");
        pathBuilder.permissionName("some-name");
        pathBuilder.put();
        var enhancedConfig = builder.build();
        var enhancedPath = enhancedConfig.policyEnforcer().paths().get("2");
        assertEquals(ENFORCING, enhancedPath.enforcementMode());
        assertEquals("/dynamic-permission-tenant", enhancedPath.paths().orElse(List.of()).get(0));
        assertNotNull(enhancedConfig.policyEnforcer().paths().get("3"));
        assertEquals("some-name", enhancedConfig.policyEnforcer().paths().get("3").name().orElse(null));
    }

    private static void assertBuilderShortcuts() {
        var config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/path-one").patch("scope1").build();
        assertMethod(config, "PATCH", "/path-one", "scope1");
        config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/path-two").put("scope2").build();
        assertMethod(config, "PUT", "/path-two", "scope2");
        config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/path-three").post("scope3").build();
        assertMethod(config, "POST", "/path-three", "scope3");
        config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/path-four").get("scope4").build();
        assertMethod(config, "GET", "/path-four", "scope4");
        config = KeycloakPolicyEnforcerTenantConfig.builder().paths("/path-five").head("scope5").build();
        assertMethod(config, "HEAD", "/path-five", "scope5");
    }

    private static void assertMethod(KeycloakPolicyEnforcerTenantConfig config, String method, String path, String scope) {
        assertTrue(config.policyEnforcer().paths().containsKey(StringUtil.hyphenate(path)));
        assertTrue(config.policyEnforcer().paths().get(StringUtil.hyphenate(path)).paths().orElse(List.of()).contains(path));
        var pathMethod = config.policyEnforcer().paths().get(StringUtil.hyphenate(path)).methods().get(method.toLowerCase());
        assertEquals(method, pathMethod.method());
        assertTrue(pathMethod.scopes().contains(scope));
    }

    private static void assertPathCacheConfigOnly() {
        // lifespan only
        KeycloakPolicyEnforcerTenantConfig config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache(5L)
                .build();
        assertEquals(5L, config.policyEnforcer().pathCache().lifespan());
        // test default value is kept for the max entries property
        assertEquals(1000, config.policyEnforcer().pathCache().maxEntries());
        // max entries only; original config is enhanced to test that previous value is kept
        config = KeycloakPolicyEnforcerTenantConfig
                .builder(config)
                .pathCache(2)
                .build();
        assertEquals(5L, config.policyEnforcer().pathCache().lifespan());
        assertEquals(2, config.policyEnforcer().pathCache().maxEntries());
        // both lifespan and max entries
        config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache(2, 5L)
                .build();
        assertEquals(5L, config.policyEnforcer().pathCache().lifespan());
        assertEquals(2, config.policyEnforcer().pathCache().maxEntries());
        // builder
        config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache()
                .build().build();
        assertNotNull(config);
        assertNotNull(config.policyEnforcer().pathCache());
        // check defaults in place
        assertEquals(1000, config.policyEnforcer().pathCache().maxEntries());
        assertEquals(30000, config.policyEnforcer().pathCache().lifespan());
        config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache()
                .maxEntries(123)
                .build().build();
        assertEquals(123, config.policyEnforcer().pathCache().maxEntries());
        assertEquals(30000, config.policyEnforcer().pathCache().lifespan());
        config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache()
                .lifespan(321)
                .build().build();
        assertEquals(1000, config.policyEnforcer().pathCache().maxEntries());
        assertEquals(321, config.policyEnforcer().pathCache().lifespan());
        config = KeycloakPolicyEnforcerTenantConfig
                .builder()
                .pathCache()
                .maxEntries(666)
                .lifespan(555)
                .build().build();
        assertEquals(666, config.policyEnforcer().pathCache().maxEntries());
        assertEquals(555, config.policyEnforcer().pathCache().lifespan());
    }

    private static void assertClaimInformationPointConfigOnly() {
        var config = KeycloakPolicyEnforcerTenantConfig.builder()
                .claimInformationPoint(Map.of("simple0", Map.of()))
                .build();
        assertNotNull(config.policyEnforcer().claimInformationPoint());
        assertNotNull(config.policyEnforcer().claimInformationPoint().simpleConfig());
        assertFalse(config.policyEnforcer().claimInformationPoint().simpleConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().simpleConfig().containsKey("simple0"));
        config = KeycloakPolicyEnforcerTenantConfig.builder().build();
        assertNotNull(config.policyEnforcer().claimInformationPoint());
        assertNotNull(config.policyEnforcer().claimInformationPoint().complexConfig());
        assertTrue(config.policyEnforcer().claimInformationPoint().complexConfig().isEmpty());
        assertNotNull(config.policyEnforcer().claimInformationPoint().simpleConfig());
        assertTrue(config.policyEnforcer().claimInformationPoint().simpleConfig().isEmpty());
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .claimInformationPoint()
                .complexConfig(Map.of("complex1", Map.of()))
                .build().build();
        assertNotNull(config.policyEnforcer().claimInformationPoint());
        assertNotNull(config.policyEnforcer().claimInformationPoint().complexConfig());
        assertFalse(config.policyEnforcer().claimInformationPoint().complexConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().complexConfig().containsKey("complex1"));
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .claimInformationPoint()
                .simpleConfig(Map.of("simple1", Map.of()))
                .build().build();
        assertNotNull(config.policyEnforcer().claimInformationPoint());
        assertNotNull(config.policyEnforcer().claimInformationPoint().simpleConfig());
        assertFalse(config.policyEnforcer().claimInformationPoint().simpleConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().simpleConfig().containsKey("simple1"));
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .claimInformationPoint()
                .simpleConfig(Map.of("simple2", Map.of()))
                .complexConfig(Map.of("complex2", Map.of()))
                .build().build();
        assertNotNull(config.policyEnforcer().claimInformationPoint());
        assertNotNull(config.policyEnforcer().claimInformationPoint().simpleConfig());
        assertFalse(config.policyEnforcer().claimInformationPoint().simpleConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().simpleConfig().containsKey("simple2"));
        assertNotNull(config.policyEnforcer().claimInformationPoint().complexConfig());
        assertFalse(config.policyEnforcer().claimInformationPoint().complexConfig().isEmpty());
        assertTrue(config.policyEnforcer().claimInformationPoint().complexConfig().containsKey("complex2"));
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("one")
                .claimInformationPoint()
                .complexConfig(Map.of("complex3", Map.of()))
                .simpleConfig(Map.of("simple3", Map.of()))
                .build()
                .parent().build();
        assertNotNull(config.policyEnforcer().paths().get("one"));
        assertNotNull(config.policyEnforcer().paths().get("one").claimInformationPoint());
        var claimInfoPointConfig = config.policyEnforcer().paths().get("one").claimInformationPoint();
        assertFalse(claimInfoPointConfig.simpleConfig().isEmpty());
        assertTrue(claimInfoPointConfig.simpleConfig().containsKey("simple3"));
        assertNotNull(claimInfoPointConfig.complexConfig());
        assertFalse(claimInfoPointConfig.complexConfig().isEmpty());
        assertTrue(claimInfoPointConfig.complexConfig().containsKey("complex3"));
    }

    private static void assertMethodConfigOnly() {
        Assertions.assertThrows(NullPointerException.class, () -> KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("two")
                .method()
                .build());
        var config = KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("three")
                .method()
                .method("one")
                .build()
                .parent()
                .build();
        var path = config.policyEnforcer().paths().get("three");
        assertNotNull(path);
        assertFalse(path.methods().isEmpty());
        var method = path.methods().get("one");
        assertNotNull(method);
        assertEquals("one", method.method());
        // assert defaults
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.ALL, method.scopesEnforcementMode());
        assertNotNull(method.scopes());
        assertTrue(method.scopes().isEmpty());
        config = KeycloakPolicyEnforcerTenantConfig.builder()
                .paths("four")
                .method()
                .method("two")
                .scopes("one", "two")
                .scopesEnforcementMode(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED)
                .build()
                .parent()
                .build();
        path = config.policyEnforcer().paths().get("four");
        assertNotNull(path);
        assertFalse(path.methods().isEmpty());
        method = path.methods().get("two");
        assertNotNull(method);
        assertEquals("two", method.method());
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, method.scopesEnforcementMode());
        assertNotNull(method.scopes());
        assertFalse(method.scopes().isEmpty());
        assertTrue(method.scopes().contains("one"));
        assertTrue(method.scopes().contains("two"));
    }
}
