package io.quarkus.oidc.runtime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
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
        OidcConfig cfg = new OidcConfig();
        cfg.tokenCache.maxSize = 2;
        return cfg;
    }
}
