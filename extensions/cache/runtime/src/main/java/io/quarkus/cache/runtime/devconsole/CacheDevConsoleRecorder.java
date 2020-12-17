package io.quarkus.cache.runtime.devconsole;

import java.util.Optional;

import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.CaffeineCacheSupplier;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil.FlashMessageStatus;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CacheDevConsoleRecorder {

    public Handler<RoutingContext> clearCacheHandler() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String cacheName = form.get("name");
                Optional<Cache> cache = CaffeineCacheSupplier.cacheManager().getCache(cacheName);
                if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
                    ((CaffeineCache) cache.get()).invalidateAll();
                    // redirect to the same page so we can make sure we see the updated results
                    flashMessage(event, "Cache for " + cacheName + " cleared");
                    return;
                }
                flashMessage(event, "Cache for " + cacheName + " not found", FlashMessageStatus.ERROR);
            }
        };
    }
}
