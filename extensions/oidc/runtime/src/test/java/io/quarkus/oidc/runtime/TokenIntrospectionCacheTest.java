package io.quarkus.oidc.runtime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.TokenIntrospection;

public class TokenIntrospectionCacheTest {
    DefaultTokenIntrospectionUserInfoCache cache = new DefaultTokenIntrospectionUserInfoCache(createOidcConfig(), null);

    @Test
    public void testExpiredIntrospection() {
        io.quarkus.oidc.OidcTenantConfig tenantConfig = createTenantConfig("tenant-a");

        TokenIntrospection introspectionValidFor10secs = new TokenIntrospection(
                "{\"active\": true,"
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 10) + "}");
        TokenIntrospection introspectionValidFor3secs = new TokenIntrospection(
                "{\"active\": true,"
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 3) + "}");
        cache.addIntrospection("tokenValidFor10secs", introspectionValidFor10secs, tenantConfig, null);
        cache.addIntrospection("tokenValidFor3secs", introspectionValidFor3secs, tenantConfig, null);

        assertNotNull(cache.getIntrospection("tokenValidFor10secs", tenantConfig, null).await().indefinitely());
        assertNotNull(cache.getIntrospection("tokenValidFor3secs", tenantConfig, null).await().indefinitely());

        await().atMost(Duration.ofSeconds(5)).pollInterval(1, TimeUnit.SECONDS)
                .until(new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        return cache.getIntrospection("tokenValidFor3secs", tenantConfig, null).await()
                                .indefinitely() == null;
                    }

                });

        assertNotNull(cache.getIntrospection("tokenValidFor10secs", tenantConfig, null).await().indefinitely());
        assertNull(cache.getIntrospection("tokenValidFor3secs", tenantConfig, null).await().indefinitely());
    }

    @Test
    public void testCompositeKeyTenantIsolation() {
        io.quarkus.oidc.OidcTenantConfig tenantOne = createTenantConfig("tenant-one");
        io.quarkus.oidc.OidcTenantConfig tenantTwo = createTenantConfig("tenant-two");

        TokenIntrospection introspection = new TokenIntrospection(
                "{\"active\": true,"
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 300) + "}");

        // Add introspection for tenant-one
        cache.addIntrospection("token", introspection, tenantOne, null);
        assertEquals(1, cache.getCacheSize());

        // Same token for tenant-one should return the cached entry
        assertNotNull(cache.getIntrospection("token", tenantOne, null).await().indefinitely());

        // Same token for tenant-two should not return the cached entry
        assertNull(cache.getIntrospection("token", tenantTwo, null).await().indefinitely());

        // Add introspection for tenant-two with the same token
        cache.addIntrospection("token", introspection, tenantTwo, null);
        assertEquals(2, cache.getCacheSize());

        // Both tenants should have their own cached entries
        assertNotNull(cache.getIntrospection("token", tenantOne, null).await().indefinitely());
        assertNotNull(cache.getIntrospection("token", tenantTwo, null).await().indefinitely());

        // Verify the composite keys
        assertTrue(cache.containsCacheKey("tenant-one", "token"));
        assertTrue(cache.containsCacheKey("tenant-two", "token"));
    }

    private static io.quarkus.oidc.OidcTenantConfig createTenantConfig(String tenantId) {
        return new OidcTenantConfigBuilder().tenantId(tenantId).build();
    }

    private static OidcConfig createOidcConfig() {
        record OidcConfigImpl(OidcTenantConfig defaultTenant, Map<String, OidcTenantConfig> namedTenants, TokenCache tokenCache,
                boolean resolveTenantsWithIssuer, int priority) implements OidcConfig {
        }
        record TokenCacheImpl(int maxSize, Duration timeToLive,
                Optional<Duration> cleanUpTimerInterval) implements OidcConfig.TokenCache {
        }
        var tokenCache = new TokenCacheImpl(2, Duration.ofMinutes(3), Optional.empty());
        return new OidcConfigImpl(null, Map.of(), tokenCache, false, 0);
    }
}
