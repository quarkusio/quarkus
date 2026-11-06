package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.UserInfo;
import io.smallrye.config.ConfigInstanceBuilder;

public class UserInfoCacheTest {
    DefaultTokenIntrospectionUserInfoCache cache = new DefaultTokenIntrospectionUserInfoCache(createOidcConfig(), null);

    @Test
    public void testCompositeKeyTenantIsolation() {
        io.quarkus.oidc.OidcTenantConfig tenantOne = createTenantConfig("tenant-one");
        io.quarkus.oidc.OidcTenantConfig tenantTwo = createTenantConfig("tenant-two");

        UserInfo userInfoOne = new UserInfo("{\"sub\": \"alice\", \"client_id\": \"client-one\"}");

        // Add userinfo for tenant-one
        cache.addUserInfo("token", userInfoOne, tenantOne, null);
        assertEquals(1, cache.getCacheSize());

        // Same token for tenant-one should return the cached entry
        assertNotNull(cache.getUserInfo("token", tenantOne, null).await().indefinitely());

        // Same token for tenant-two should not return the cached entry
        assertNull(cache.getUserInfo("token", tenantTwo, null).await().indefinitely());

        // Add userinfo for tenant-two with the same token
        UserInfo userInfoTwo = new UserInfo("{\"sub\": \"alice\", \"client_id\": \"client-two\"}");
        cache.addUserInfo("token", userInfoTwo, tenantTwo, null);
        assertEquals(2, cache.getCacheSize());

        // Both tenants should have their own cached entries
        assertNotNull(cache.getUserInfo("token", tenantOne, null).await().indefinitely());
        assertNotNull(cache.getUserInfo("token", tenantTwo, null).await().indefinitely());

        // Verify each tenant gets its own userinfo
        assertEquals("client-one",
                cache.getUserInfo("token", tenantOne, null).await().indefinitely().getString("client_id"));
        assertEquals("client-two",
                cache.getUserInfo("token", tenantTwo, null).await().indefinitely().getString("client_id"));

        // Verify the composite keys
        assertTrue(cache.containsCacheKey("tenant-one", "token"));
        assertTrue(cache.containsCacheKey("tenant-two", "token"));
    }

    private static io.quarkus.oidc.OidcTenantConfig createTenantConfig(String tenantId) {
        return new OidcTenantConfigBuilder().tenantId(tenantId).build();
    }

    private static OidcConfig createOidcConfig() {
        return ConfigInstanceBuilder.forInterface(OidcConfig.class)
                .with(OidcConfig::namedTenants, Map.<String, OidcTenantConfig> of())
                .with(OidcConfig::tokenCache, ConfigInstanceBuilder.forInterface(OidcConfig.TokenCache.class)
                        .with(OidcConfig.TokenCache::maxSize, 2)
                        .with(OidcConfig.TokenCache::timeToLive, Duration.ofMinutes(3))
                        .build())
                .with(OidcConfig::resolveTenantsWithIssuer, false)
                .with(OidcConfig::priority, 0)
                .build();
    }
}
