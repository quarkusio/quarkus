package io.quarkus.cache.runtime.devconsole;

import java.util.Optional;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.CaffeineCacheSupplier;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.Json;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CacheDevConsoleRecorder {
    public Handler<RoutingContext> clearCacheHandler() {
        return new DevConsolePostHandler() {
            int code;
            String message = "";

            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String cacheName = form.get("name");
                Optional<Cache> cache = CaffeineCacheSupplier.cacheManager().getCache(cacheName);
                if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
                    CaffeineCache caffeineCache = (CaffeineCache) cache.get();

                    String action = form.get("action");
                    if (action.equalsIgnoreCase("clearCache")) {
                        caffeineCache.invalidateAll();
                    }
                    this.code = HttpResponseStatus.OK.code();
                    this.message = createResponseMessage(caffeineCache);
                    return;
                } else {
                    String errorMessage = "Cache for " + cacheName + " not found";
                    this.code = HttpResponseStatus.NOT_FOUND.code();
                    this.message = createResponseError(cacheName, errorMessage);
                }
            }

            @Override
            protected void actionSuccess(RoutingContext event) {
                event.response().setStatusCode(this.code);
                event.response().end(this.message);
            }

            private String createResponseMessage(CaffeineCache cache) {
                Json.JsonObjectBuilder object = Json.object();
                object.put("name", cache.getName());
                object.put("size", cache.getSize());
                return object.build();
            }

            private String createResponseError(String name, String error) {
                Json.JsonObjectBuilder object = Json.object();
                object.put("name", name);
                object.put("error", error);
                return object.build();
            }
        };
    }
}
