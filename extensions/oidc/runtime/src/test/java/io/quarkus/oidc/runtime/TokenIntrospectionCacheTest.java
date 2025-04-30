package io.quarkus.oidc.runtime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;

public class TokenIntrospectionCacheTest {
    TokenIntrospectionCache cache = new DefaultTokenIntrospectionUserInfoCache(createOidcConfig(), null);

    @Test
    public void testExpiredIntrospection() {

        TokenIntrospection introspectionValidFor10secs = new TokenIntrospection(
                "{\"active\": true,"
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 10) + "}");
        TokenIntrospection introspectionValidFor3secs = new TokenIntrospection(
                "{\"active\": true,"
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 3) + "}");
        cache.addIntrospection("tokenValidFor10secs", introspectionValidFor10secs, null, null);
        cache.addIntrospection("tokenValidFor3secs", introspectionValidFor3secs, null, null);

        assertNotNull(cache.getIntrospection("tokenValidFor10secs", null, null).await().indefinitely());
        assertNotNull(cache.getIntrospection("tokenValidFor3secs", null, null).await().indefinitely());

        await().atMost(Duration.ofSeconds(5)).pollInterval(1, TimeUnit.SECONDS)
                .until(new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        return cache.getIntrospection("tokenValidFor3secs", null, null).await().indefinitely() == null;
                    }

                });

        assertNotNull(cache.getIntrospection("tokenValidFor10secs", null, null).await().indefinitely());
        assertNull(cache.getIntrospection("tokenValidFor3secs", null, null).await().indefinitely());
    }

    private static OidcConfig createOidcConfig() {
        record OidcConfigImpl(OidcTenantConfig defaultTenant, Map<String, OidcTenantConfig> namedTenants, TokenCache tokenCache,
                boolean resolveTenantsWithIssuer) implements OidcConfig {
        }
        record TokenCacheImpl(int maxSize, Duration timeToLive,
                Optional<Duration> cleanUpTimerInterval) implements OidcConfig.TokenCache {
        }
        var tokenCache = new TokenCacheImpl(2, Duration.ofMinutes(3), Optional.empty());
        return new OidcConfigImpl(null, Map.of(), tokenCache, false);
    }
}
