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
                .setPaths("/api/one").setMethod("TRACE", null)
                .setPaths("/api/two").setMethod("TRACE", null)
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
        var path = builder.setPaths("/api/one");
        path.setPermissionName("New Permission");
        path.setClaimInformationPoint(Map.of("key", Map.of("sub-key", "sub-value")));
        path.clearMethods();
        path.setGet(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, "scope5").build();
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
        assertEquals(1, p.methods().size());
        method = p.methods().get("trace");
        assertNull(method);
        method = p.methods().get("get");
        // we didn't add scopes, and we are yet to require them
        assertTrue(method.scopes().contains("scope5"));
        assertEquals("GET", method.method());
        // default scope enforcement is ALL
        assertEquals(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, method.scopesEnforcementMode());
        // in addition to the GET and PUT and POST methods
        builder = KeycloakPolicyEnforcerTenantConfig.builder(config);
        path = builder.setPaths("/api/one");
        path.setPost("1", "2");
        path.setPut(PolicyEnforcerConfig.ScopeEnforcementMode.ANY, "3", "4");
        config = builder.build();
        var pathConfig = config.policyEnforcer().paths().get("/api/one");
        assertEquals(3, pathConfig.methods().size());
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
        config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/x", "/y").setEnforcementMode(PERMISSIVE).build();
        pathConfig = config.policyEnforcer().paths().entrySet().stream().findAny().get().getValue();
        assertEquals(2, pathConfig.paths().get().size());
        assertTrue(pathConfig.paths().get().contains("/x"));
        assertTrue(pathConfig.paths().get().contains("/y"));
        assertEquals(PERMISSIVE, pathConfig.enforcementMode());
    }

    private static void assertEveryConfigPropertyCanBeSet() {
        var builder = KeycloakPolicyEnforcerTenantConfig.builder()
                .setEnforcementMode(DISABLED)
                .setClaimInformationPoint(Map.of("one", Map.of("two", "three")), Map.of("four", Map.of()))
                .setConnectionPoolSize(-1)
                .setLazyLoadPaths(false)
                .setPathCache(5, 2)
                .setHttpMethodAsScope(true);
        var pathBuilder = builder.setNamedPaths("p1", "path");
        pathBuilder.setEnforcementMode(PolicyEnforcerConfig.EnforcementMode.PERMISSIVE);
        pathBuilder.setPermissionName("n1");
        pathBuilder.setMethod("method1", PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED, "scope1", "scopes2");
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
        builder.setNamedPaths("2").setEnforcementMode(ENFORCING);
        var pathBuilder = builder.setNamedPaths("3");
        pathBuilder.setPermissionName("some-name");
        pathBuilder.setPut();
        var enhancedConfig = builder.build();
        var enhancedPath = enhancedConfig.policyEnforcer().paths().get("2");
        assertEquals(ENFORCING, enhancedPath.enforcementMode());
        assertEquals("/dynamic-permission-tenant", enhancedPath.paths().orElse(List.of()).get(0));
        assertNotNull(enhancedConfig.policyEnforcer().paths().get("3"));
        assertEquals("some-name", enhancedConfig.policyEnforcer().paths().get("3").name().orElse(null));
    }

    private static void assertBuilderShortcuts() {
        var config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/path-one").setPatch("scope1").build();
        assertMethod(config, "PATCH", "/path-one", "scope1");
        config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/path-two").setPut("scope2").build();
        assertMethod(config, "PUT", "/path-two", "scope2");
        config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/path-three").setPost("scope3").build();
        assertMethod(config, "POST", "/path-three", "scope3");
        config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/path-four").setGet("scope4").build();
        assertMethod(config, "GET", "/path-four", "scope4");
        config = KeycloakPolicyEnforcerTenantConfig.builder().setPaths("/path-five").setHead("scope5").build();
        assertMethod(config, "HEAD", "/path-five", "scope5");
    }

    private static void assertMethod(KeycloakPolicyEnforcerTenantConfig config, String method, String path, String scope) {
        assertTrue(config.policyEnforcer().paths().containsKey(StringUtil.hyphenate(path)));
        assertTrue(config.policyEnforcer().paths().get(StringUtil.hyphenate(path)).paths().orElse(List.of()).contains(path));
        var pathMethod = config.policyEnforcer().paths().get(StringUtil.hyphenate(path)).methods().get(method.toLowerCase());
        assertEquals(method, pathMethod.method());
        assertTrue(pathMethod.scopes().contains(scope));
    }
}
